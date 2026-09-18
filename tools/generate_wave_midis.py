#!/usr/bin/env python3
from pathlib import Path
import struct, zipfile, math, random, shutil

OUT = Path("generated_music")
MID = OUT / "mid"
MID.mkdir(parents=True, exist_ok=True)
TPB = 480

def vlq(n):
    n=max(0,int(n)); a=[n&0x7f]
    while n>>7:
        n>>=7; a.append((n&0x7f)|0x80)
    return bytes(reversed(a))

def msg(delta,status,d1=None,d2=None):
    b=bytearray(vlq(delta)); b.append(status&0xff)
    if d1 is not None: b.append(d1&0x7f)
    if d2 is not None: b.append(d2&0x7f)
    return bytes(b)

def meta(delta,typ,data=b""):
    return vlq(delta)+bytes([0xff,typ])+vlq(len(data))+data

def note(ev,beat,dur,n,v,ch):
    if not 0<=n<=127:return
    a=round(beat*TPB); b=round((beat+max(.04,dur))*TPB)
    ev.append((a,2,lambda d,n=n,v=v,ch=ch:msg(d,0x90|ch,n,v)))
    ev.append((b,1,lambda d,n=n,ch=ch:msg(d,0x80|ch,n,0)))

def chord(ev,beat,dur,notes,v,ch,spread=.0):
    for i,n in enumerate(notes): note(ev,beat+i*spread,dur,n,max(1,v-i*2),ch)

def cc(ev,beat,ch,num,val):
    t=round(beat*TPB)
    ev.append((t,0,lambda d,ch=ch,num=num,val=val:msg(d,0xb0|ch,num,val)))

def track(name,ch=None,program=None,pan=64,vol=100,ev=None):
    e=[(0,0,lambda d,n=name:meta(d,0x03,n.encode("utf-8")))]
    if ch is not None:
        if program is not None and ch!=9:e.append((0,0,lambda d,ch=ch,p=program:msg(d,0xc0|ch,p)))
        e.append((0,0,lambda d,ch=ch,v=pan:msg(d,0xb0|ch,10,v)))
        e.append((0,0,lambda d,ch=ch,v=vol:msg(d,0xb0|ch,7,v)))
        e.append((0,0,lambda d,ch=ch:msg(d,0xb0|ch,91,34 if ch!=9 else 12)))
    e.extend(ev or []); e.sort(key=lambda x:(x[0],x[1]))
    out=bytearray(); last=0
    for tick,_,fn in e: out.extend(fn(tick-last)); last=tick
    out.extend(meta(0,0x2f))
    return b"MTrk"+struct.pack(">I",len(out))+out

def tempo_track(bpm,title):
    us=round(60000000/bpm)
    e=[
      (0,0,lambda d,t=title:meta(d,0x03,t.encode("utf-8"))),
      (0,0,lambda d:meta(d,0x02,b"Original para Asteroide Matematico")),
      (0,0,lambda d,u=us:meta(d,0x51,u.to_bytes(3,"big"))),
      (0,0,lambda d:meta(d,0x58,bytes([4,2,24,8])))
    ]
    out=bytearray(); last=0
    for tick,_,fn in e: out.extend(fn(tick-last)); last=tick
    out.extend(meta(0,0x2f))
    return b"MTrk"+struct.pack(">I",len(out))+out

def write(path,bpm,title,tracks):
    chunks=[tempo_track(bpm,title)]+tracks
    path.write_bytes(b"MThd"+struct.pack(">IHHH",6,1,len(chunks),TPB)+b"".join(chunks))

ROOTS={"C":60,"C#":61,"D":62,"Eb":63,"E":64,"F":65,"F#":66,"G":67,"Ab":68,"A":69,"Bb":70,"B":71}
MINOR=[[0,5,2,6],[0,6,5,6],[0,3,6,0],[0,2,5,6],[0,5,3,6],[0,6,2,5]]
MAJOR=[[0,4,5,3],[0,3,4,0],[5,3,0,4],[0,5,3,4],[3,0,4,5],[0,2,3,4]]
SPECS=[
("D","minor",116,"Sentinela do Horizonte"),
("F","major",118,"Primeira Muralha"),
("E","minor",119,"Marcha das Torres"),
("G","major",120,"Ceu em Alerta"),
("A","minor",121,"Linha de Defesa"),
("D","major",122,"Guarda de Aco"),
("F#","minor",123,"Pulso de Emergencia"),
("Bb","major",124,"Cidade Resiste"),
("C","minor",125,"Cerco Celeste"),
("E","major",126,"Contra-Ataque"),
("G","minor",127,"Estandarte de Fogo"),
("A","major",128,"Avanco das Baterias"),
("B","minor",129,"Noite de Impacto"),
("D","minor",130,"Escudo em Chamas"),
("F","major",131,"Vanguarda Urbana"),
("C#","minor",132,"Sirene de Combate"),
("E","minor",134,"Coracao da Fortaleza"),
("G","major",136,"Asas da Resistencia"),
("A","minor",138,"Tempestade de Ferro"),
("B","major",140,"Comando de Defesa"),
("C#","minor",142,"Ultima Linha"),
("D","major",144,"Ceu Invencivel"),
("E","minor",146,"Marcha Final"),
("F#","minor",148,"Vitoria sob as Estrelas")
]

def triad(root,deg,mode):
    scale=[0,2,3,5,7,8,10] if mode=="minor" else [0,2,4,5,7,9,11]
    quals=["m","d","M","m","m","M","M"] if mode=="minor" else ["M","m","m","M","M","m","d"]
    r=root+scale[deg]; q=quals[deg]
    ints=[0,4,7] if q=="M" else ([0,3,7] if q=="m" else [0,3,6])
    return r,[r+i for i in ints]

def power(ev,b,d,r,v,ch):
    chord(ev,b,d,[r,r+7,r+12],v,ch,.005)

def make_wave(index,root_name,mode,bpm,title):
    # index 0..23 -> files onda_02..onda_25
    wave=index+2
    rng=random.Random(1000+(index+1)*7919)
    root=ROOTS[root_name]; hr=root-12
    prog=(MINOR if mode=="minor" else MAJOR)[index%6]
    progB=(MINOR if mode=="minor" else MAJOR)[(index+2)%6]
    intensity=index/23.0
    lead=[]; gl=[]; gr=[]; bass=[]; brass=[]; strings=[]; drums=[]
    for chn,ev in [(0,lead),(1,gl),(2,gr),(3,bass),(4,brass),(5,strings)]:
        cc(ev,0,chn,11,100 if chn else 110)

    scaleints=[0,2,3,5,7,8,10] if mode=="minor" else [0,2,4,5,7,9,11]
    rhythm_bank=[[0,.75,1.5,2.25,3],[0,.5,1,2,2.5,3.25],[0,1,1.5,2.5,3],[0,.5,1.5,2,3,3.5]]
    contour_bank=[[0,2,4,3,2,4],[0,4,5,4,2,1],[2,3,4,6,4,2],[0,1,3,4,5,3],[4,3,2,0,2,4],[0,3,5,6,4,2]]
    rhy=rhythm_bank[index%len(rhythm_bank)]; contour=contour_bank[index%len(contour_bank)]

    lastlead=root+24
    for bar in range(32):
        b0=bar*4; p=progB if 16<=bar<24 else prog
        rr,ch=triad(hr,p[bar%4],mode)

        chord(strings,b0,3.92,[n+12 for n in ch],42+int(14*intensity),5,.012)

        if index<4:
            boffs=[0,2]; rpat=[0,2]; rdur=.70
        elif index<10:
            boffs=[0,2]; rpat=[0,1,2,3]; rdur=.50
        else:
            boffs=[0,1.5,2.5]; rpat=[0,.5,1,1.5,2,2.5,3,3.5]; rdur=.31
        for off in boffs: chord(brass,b0+off,.62,[n+24 for n in ch],50+int(25*intensity),4,.008)
        for k,off in enumerate(rpat):
            power(gl,b0+off,rdur,rr-12,55+int(22*intensity)+(7 if off in (0,2) else 0),1)
            power(gr,b0+off+.018,rdur*.94,rr-12,50+int(22*intensity)+(7 if off in (0,2) else 0),2)
        bpat=[0,1,2,3] if index<7 else [0,.5,1,1.5,2,2.5,3,3.5]
        for i,off in enumerate(bpat):
            note(bass,b0+off,.42 if len(bpat)>4 else .75,rr-24 if i%4!=3 else rr-17,76+int(14*intensity),3)

        step=.5 if index<12 else .25
        h=0
        while h<4:
            note(drums,b0+h,.04,42,46+(8 if abs(h-round(h))<.01 else 0)+int(6*intensity),9); h+=step
        kicks=[0,2] if index<4 else [0,1.5,2,3.25]
        if index>=14:kicks=[0,.75,1.5,2,2.75,3.5]
        for off in kicks: note(drums,b0+off,.08,36,84+int(18*intensity),9)
        for off in [1,3]: note(drums,b0+off,.09,38,88+int(15*intensity),9)
        if bar%4==0 and index>=4: note(drums,b0,.15,49,78+int(14*intensity),9)
        if bar in (7,15,23,31):
            for j,n in enumerate([45,47,48,50]): note(drums,b0+3+j*.23,.08,n,74+j*3+int(14*intensity),9)

        use=rhy if bar%2==0 else rhy[:-1]
        for j,off in enumerate(use):
            deg=contour[(j+bar)%len(contour)]%7
            target=root+12+scaleints[deg]+(5 if 16<=bar<24 and mode=="minor" else (7 if 16<=bar<24 else 0))
            candidates=[target-12,target,target+12]
            pitch=min(candidates,key=lambda x:abs(x-lastlead))
            if bar%8==0 and j==0 and index>=7:pitch=min(91,pitch+12)
            note(lead,b0+off,.40 if j<len(use)-1 else .68,pitch,88+int(18*intensity)+rng.randint(-4,4),0)
            lastlead=pitch

    tracks=[
      track("Guitarra Solo",0,30,76,110,lead),
      track("Guitarra Base L",1,29,32,102,gl),
      track("Guitarra Base R",2,29,96,102,gr),
      track("Baixo",3,33,60,108,bass),
      track("Metais",4,61,66,100,brass),
      track("Cordas",5,48,64,90,strings),
      track("Bateria GM",9,None,64,110,drums)
    ]
    path=MID/f"onda_{wave:02d}.mid"
    write(path,bpm,f"Onda {wave:02d} - {title}",tracks)
    return path

def make_victory():
    rng=random.Random(252525)
    bpm=152; root=62 # D
    lead=[]; gl=[]; gr=[]; bass=[]; brass=[]; strings=[]; drums=[]
    prog=[0,4,5,3] # D-A-Bm-G
    scale=[0,2,4,5,7,9,11]
    last=86
    for bar in range(40):
        b0=bar*4; deg=prog[bar%4]
        rr,ch=triad(root-12,deg,"major")
        chord(strings,b0,3.95,[n+12 for n in ch],62,5,.01)
        chord(brass,b0,.75,[n+24 for n in ch],88,4,.006)
        chord(brass,b0+2,.55,[n+24 for n in ch],78,4,.006)
        for off in [0,.5,1,1.5,2,2.5,3,3.5]:
            power(gl,b0+off,.34,rr-12,78+(8 if off in (0,2) else 0),1)
            power(gr,b0+off+.018,.32,rr-12,73+(8 if off in (0,2) else 0),2)
            note(bass,b0+off,.40,rr-24 if int(off*2)%4!=3 else rr-17,92,3)
        for off in [0,.5,1,1.5,2,2.5,3,3.5]: note(drums,b0+off,.04,42,58,9)
        for off in [0,1.5,2,2.75,3.5]: note(drums,b0+off,.08,36,104,9)
        for off in [1,3]: note(drums,b0+off,.08,38,108,9)
        if bar%4==0: note(drums,b0,.18,49,102,9)

        motif=[0,2,4,5,4,2,6,4] if (bar//4)%2==0 else [4,5,6,4,2,3,4,1]
        offs=[0,.5,1,1.5,2,2.5,3,3.5]
        for j,off in enumerate(offs):
            target=root+12+scale[motif[j]%7]
            cand=[target-12,target,target+12]
            pitch=min(cand,key=lambda x:abs(x-last))
            if bar in (8,16,24,32) and j==0:pitch=min(93,pitch+12)
            note(lead,b0+off,.38,pitch,108-rng.randint(0,8),0); last=pitch

    # grande acorde final sustentado
    end=40*4
    chord(strings,end,6,[62,66,69,74],82,5,.015)
    chord(brass,end,4,[74,78,81],112,4,.008)
    chord(lead,end,3,[86,90,93],112,0,.012)
    chord(gl,end,4,[50,57,62],95,1,.006)
    chord(gr,end+.018,4,[50,57,62],90,2,.006)
    note(bass,end,4,38,112,3)
    note(drums,end,.3,49,120,9)

    tracks=[
      track("Guitarra Solo",0,30,76,112,lead),
      track("Guitarra Base L",1,29,32,104,gl),
      track("Guitarra Base R",2,29,96,104,gr),
      track("Baixo",3,33,60,110,bass),
      track("Metais",4,61,66,108,brass),
      track("Cordas",5,48,64,96,strings),
      track("Bateria GM",9,None,64,112,drums)
    ]
    path=MID/"vitoria_final.mid"
    write(path,bpm,"Vitoria Final - Cidade Salva",tracks)
    return path

for i,spec in enumerate(SPECS): make_wave(i,*spec)
make_victory()

bat=r'''@echo off
setlocal EnableExtensions EnableDelayedExpansion
title Asteroide Matematico - MIDI para OGG

REM Coloque/extraia este pacote na mesma pasta do fluidsynth.exe
REM e do arquivo "Timbres Of Heaven GM_GS_XG_SFX V 3.4 Final.sf2".
REM O FFmpeg pode estar nesta pasta ou no PATH do Windows.

set "BASE=%~dp0"
set "SF2=%BASE%Timbres Of Heaven GM_GS_XG_SFX V 3.4 Final.sf2"
set "FLUID=%BASE%fluidsynth.exe"
set "FFMPEG=%BASE%ffmpeg.exe"

if not exist "%FLUID%" (
    where fluidsynth >nul 2>nul
    if errorlevel 1 (
        echo ERRO: fluidsynth.exe nao encontrado nesta pasta nem no PATH.
        pause
        exit /b 1
    )
    set "FLUID=fluidsynth"
)

if not exist "%SF2%" (
    echo ERRO: SoundFont nao encontrado:
    echo "%SF2%"
    echo.
    echo O nome esperado e exatamente:
    echo Timbres Of Heaven GM_GS_XG_SFX V 3.4 Final.sf2
    pause
    exit /b 1
)

if not exist "%FFMPEG%" (
    where ffmpeg >nul 2>nul
    if errorlevel 1 (
        echo ERRO: ffmpeg.exe nao encontrado nesta pasta nem no PATH.
        pause
        exit /b 1
    )
    set "FFMPEG=ffmpeg"
)

set "MID=%BASE%mid"
set "TMP=%BASE%wav_temp"
set "OGG=%BASE%ogg"

if not exist "%TMP%" mkdir "%TMP%"
if not exist "%OGG%" mkdir "%OGG%"

echo.
echo Convertendo ondas 02 a 25...
echo.

for %%F in ("%MID%\onda_*.mid") do (
    echo [FluidSynth] %%~nxF
    "%FLUID%" -ni -g 0.70 -r 44100 -F "%TMP%\%%~nF.wav" "%SF2%" "%%F"
    if errorlevel 1 goto :erro

    echo [FFmpeg] %%~nF.ogg
    "%FFMPEG%" -y -hide_banner -loglevel warning -i "%TMP%\%%~nF.wav" -af "loudnorm=I=-16:TP=-1.5:LRA=11" -c:a libvorbis -q:a 5 "%OGG%\%%~nF.ogg"
    if errorlevel 1 goto :erro
    del "%TMP%\%%~nF.wav"
)

echo [FluidSynth] vitoria_final.mid
"%FLUID%" -ni -g 0.70 -r 44100 -F "%TMP%\vitoria_final.wav" "%SF2%" "%MID%\vitoria_final.mid"
if errorlevel 1 goto :erro

echo [FFmpeg] vitoria_final.ogg
"%FFMPEG%" -y -hide_banner -loglevel warning -i "%TMP%\vitoria_final.wav" -af "loudnorm=I=-16:TP=-1.5:LRA=11" -c:a libvorbis -q:a 5 "%OGG%\vitoria_final.ogg"
if errorlevel 1 goto :erro
del "%TMP%\vitoria_final.wav"

rmdir "%TMP%" 2>nul
echo.
echo ============================================================
echo CONCLUIDO
echo Os OGGs estao em:
echo "%OGG%"
echo.
echo Copie onda_02.ogg ate onda_25.ogg e vitoria_final.ogg
echo para assets\audio do projeto.
echo ============================================================
pause
exit /b 0

:erro
echo.
echo ERRO durante a conversao. Verifique a mensagem acima.
pause
exit /b 1
'''
(OUT/"converter_todos_para_ogg.bat").write_text(bat,encoding="utf-8")

readme='''ASTEROIDE MATEMATICO - MUSICAS DAS ONDAS 02 A 25
=================================================

Arquivos:
  mid/onda_02.mid ... mid/onda_25.mid
  mid/vitoria_final.mid
  converter_todos_para_ogg.bat

A onda 01 continua usando a musica que ja existe no jogo:
  assets/audio/musica_jogo.ogg

COMO CONVERTER
1. Extraia este ZIP na mesma pasta onde estao:
     fluidsynth.exe
     Timbres Of Heaven GM_GS_XG_SFX V 3.4 Final.sf2

2. O ffmpeg.exe deve estar na mesma pasta OU instalado no PATH.

3. Execute:
     converter_todos_para_ogg.bat

4. O BAT criara a pasta "ogg" com:
     onda_02.ogg ... onda_25.ogg
     vitoria_final.ogg

5. Copie esses 25 OGGs para:
     assets/audio/

O jogo ja foi preparado para:
- musica_jogo.ogg na onda 01;
- onda_02.ogg ... onda_25.ogg nas ondas correspondentes;
- vitoria_final.ogg ao vencer a onda 25.

Render:
- FluidSynth 44,1 kHz;
- Timbres Of Heaven;
- OGG Vorbis qualidade 5;
- normalizacao aproximada para -16 LUFS e pico -1,5 dB.
'''
(OUT/"LEIA-ME.txt").write_text(readme,encoding="utf-8")

zip_path=Path("asteroide_matematico_ondas_02_25_mid.zip")
with zipfile.ZipFile(zip_path,"w",zipfile.ZIP_DEFLATED) as z:
    for p in sorted(OUT.rglob("*")):
        if p.is_file(): z.write(p,p.relative_to(OUT))

# validations
files=sorted(MID.glob("*.mid"))
assert len(files)==25, len(files)
assert (MID/"onda_02.mid").exists()
assert (MID/"onda_25.mid").exists()
assert (MID/"vitoria_final.mid").exists()
for p in files:
    data=p.read_bytes()
    assert data[:4]==b"MThd" and b"MTrk" in data

print(zip_path)
