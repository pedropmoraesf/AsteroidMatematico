#!/usr/bin/env python3
from pathlib import Path
import struct, zipfile, random, math, shutil

TPB=480
OUT=Path("generated_music")
MID=OUT/"mid"
if OUT.exists(): shutil.rmtree(OUT)
MID.mkdir(parents=True)

def vlq(n):
    n=max(0,int(n)); a=[n&127]
    while n>>7:
        n>>=7; a.append((n&127)|128)
    return bytes(reversed(a))

def meta(d,t,data=b""):
    return vlq(d)+bytes([255,t])+vlq(len(data))+data

def msg(d,s,a=None,b=None):
    o=bytearray(vlq(d)); o.append(s&255)
    if a is not None:o.append(a&127)
    if b is not None:o.append(b&127)
    return bytes(o)

def note(ev,beat,dur,pitch,vel,ch):
    if not 0<=pitch<=127:return
    st=round(beat*TPB); en=round((beat+max(.04,dur))*TPB)
    ev.append((st,2,lambda d,p=pitch,v=vel,c=ch:msg(d,0x90|c,p,v)))
    ev.append((en,1,lambda d,p=pitch,c=ch:msg(d,0x80|c,p,0)))

def chord(ev,beat,dur,notes,vel,ch,spread=0):
    for i,n in enumerate(notes): note(ev,beat+i*spread,dur,n,max(1,vel-i*3),ch)

def cc(ev,beat,ch,num,val):
    t=round(beat*TPB)
    ev.append((t,0,lambda d,c=ch,n=num,v=val:msg(d,0xB0|c,n,v)))

def track(name,ch=None,program=None,pan=64,vol=100,events=None):
    ev=[(0,0,lambda d,n=name:meta(d,3,n.encode()))]
    if ch is not None:
        if program is not None and ch!=9: ev.append((0,0,lambda d,c=ch,p=program:msg(d,0xC0|c,p)))
        ev += [
            (0,0,lambda d,c=ch,p=pan:msg(d,0xB0|c,10,p)),
            (0,0,lambda d,c=ch,v=vol:msg(d,0xB0|c,7,v)),
            (0,0,lambda d,c=ch:msg(d,0xB0|c,91,32 if c!=9 else 10)),
        ]
    ev += events or []
    ev.sort(key=lambda x:(x[0],x[1]))
    data=bytearray(); last=0
    for t,_,fn in ev:
        data.extend(fn(t-last)); last=t
    data.extend(meta(0,47,b""))
    return b"MTrk"+struct.pack(">I",len(data))+data

def tempo_track(bpm,title):
    us=round(60000000/bpm)
    ev=[
        (0,0,lambda d,t=title:meta(d,3,t.encode())),
        (0,0,lambda d:meta(d,2,b"Original - Asteroide Matematico")),
        (0,0,lambda d,u=us:meta(d,81,u.to_bytes(3,"big"))),
        (0,0,lambda d:meta(d,88,bytes([4,2,24,8]))),
    ]
    data=bytearray(); last=0
    for t,_,fn in ev:
        data.extend(fn(t-last)); last=t
    data.extend(meta(0,47,b""))
    return b"MTrk"+struct.pack(">I",len(data))+data

def save(path,bpm,title,tracks):
    chunks=[tempo_track(bpm,title)]+tracks
    path.write_bytes(b"MThd"+struct.pack(">IHHH",6,1,len(chunks),TPB)+b"".join(chunks))

ROOTS={"C":60,"C#":61,"D":62,"Eb":63,"E":64,"F":65,"F#":66,"G":67,"Ab":68,"A":69,"Bb":70,"B":71}
MINOR=[[0,5,2,6],[0,6,5,6],[0,3,6,0],[0,2,5,6],[0,5,3,6],[0,6,2,5]]
MAJOR=[[0,4,5,3],[0,3,4,0],[5,3,0,4],[0,5,3,4],[3,0,4,5],[0,2,3,4]]
SPECS=[
("D","minor",116,"Sentinela do Horizonte"),("F","major",118,"Primeira Muralha"),
("E","minor",119,"Marcha das Torres"),("G","major",120,"Ceu em Alerta"),
("A","minor",121,"Linha de Defesa"),("D","major",122,"Guarda de Aco"),
("F#","minor",123,"Pulso de Emergencia"),("Bb","major",124,"Cidade Resiste"),
("C","minor",125,"Cerco Celeste"),("E","major",126,"Contra-Ataque"),
("G","minor",127,"Estandarte de Fogo"),("A","major",128,"Avanco das Baterias"),
("B","minor",129,"Noite de Impacto"),("D","minor",130,"Escudo em Chamas"),
("F","major",131,"Vanguarda Urbana"),("C#","minor",132,"Sirene de Combate"),
("E","minor",134,"Coracao da Fortaleza"),("G","major",136,"Asas da Resistencia"),
("A","minor",138,"Tempestade de Ferro"),("B","major",140,"Comando de Defesa"),
("C#","minor",142,"Ultima Linha"),("D","major",144,"Ceu Invencivel"),
("E","minor",146,"Marcha Final"),("F#","minor",148,"Vitoria sob as Estrelas")]

RHY=[
[0,.75,1.5,2.25,3],[0,.5,1,2,2.5,3.25],[0,1,1.5,2.5,3],
[0,.5,1.5,2,3,3.5],[0,.75,1.25,2,2.75,3.5],[0,.5,1,1.5,2.5,3.25]]
CON=[
[0,2,4,3,2,4],[0,4,5,4,2,1],[2,3,4,6,4,2],[0,1,3,4,5,3],
[4,3,2,0,2,4],[0,3,5,6,4,2],[2,4,6,5,3,1],[0,2,5,4,3,6]]

def triad(root,d,mode):
    if mode=="minor":
        scale=[0,2,3,5,7,8,10]; q=["m","d","M","m","m","M","M"]
    else:
        scale=[0,2,4,5,7,9,11]; q=["M","m","m","M","M","m","d"]
    r=root+scale[d]
    ints=[0,4,7] if q[d]=="M" else ([0,3,7] if q[d]=="m" else [0,3,6])
    return r,[r+i for i in ints]

def scale(root,mode):
    return [root+i for i in ([0,2,3,5,7,8,10] if mode=="minor" else [0,2,4,5,7,9,11])]

def clamp(x,a,b): return max(a,min(b,x))
def human(rng,a=.015): return rng.uniform(-a,a)

def power(ev,b,dur,r,v,ch):
    chord(ev,b,dur,[r,r+7,r+12],v,ch,.006)

def compose(i,root_name,mode,bpm,title):
    rng=random.Random(100000+i*7919)
    root=ROOTS[root_name]; hr=root-12
    progs=MINOR if mode=="minor" else MAJOR
    pa=progs[(i-1)%6]; pb=progs[(i+1)%6]
    bars=[]
    for bar in range(24):
        prog=pb if 12<=bar<18 else pa
        degree=prog[bar%4]
        rr,ch=triad(hr,degree,mode)
        bars.append((rr,ch))

    lead=[]; g1=[]; g2=[]; bass=[]; brass=[]; strings=[]; drums=[]
    intensity=(i-1)/23
    lv=int(86+20*intensity); rv=int(52+24*intensity)
    bv=int(48+27*intensity); dv=int(80+23*intensity)

    for ev,ch in [(lead,0),(g1,1),(g2,2),(bass,3),(brass,4),(strings,5)]: cc(ev,0,ch,11,100)

    for bar,(rr,ch) in enumerate(bars):
        b0=bar*4
        sec=12<=bar<18
        if bar%3==0 or i>=8:
            chord(strings,b0+human(rng,.006),3.90,[n+12 for n in ch],42+int(14*intensity),5,.012)

        if i<=4:
            if bar%2==0: chord(brass,b0,1.6,[n+24 for n in ch],bv,4,.008)
        elif i<=12:
            for off in (0,2): chord(brass,b0+off,.70,[n+24 for n in ch],bv,4,.006)
        else:
            for off in (0,1.5,2.5): chord(brass,b0+off,.44,[n+24 for n in ch],bv,4,.005)

        if i<=4: pat=[0,2]; dur=.70
        elif i<=10: pat=[0,1,2,3]; dur=.50
        else: pat=[0,.5,1,1.5,2,2.5,3,3.5]; dur=.31
        for k,off in enumerate(pat):
            acc=7 if off in (0,2) else 0
            power(g1,b0+off+human(rng),dur,rr-12,rv+acc,1)
            power(g2,b0+off+.018+human(rng,.010),dur*.94,rr-12,rv-5+acc,2)

        bpat=[0,1,2,3] if i<8 else [0,.5,1,1.5,2,2.5,3,3.5]
        for k,off in enumerate(bpat):
            n=rr-24 if k%4!=3 else rr-17
            if sec and k%3==2:n=rr-12
            note(bass,b0+off+human(rng,.007),.42 if len(bpat)>4 else .76,n,76+int(14*intensity),3)

        hs=.5 if i<13 else .25
        h=0.0
        while h<4:
            note(drums,b0+h+human(rng,.005),.04,42,44+(8 if abs(h-round(h))<.01 else 0)+int(7*intensity),9)
            h+=hs
        kicks=[0,2] if i<5 else [0,1.5,2,3.25]
        if i>=15:kicks=[0,.75,1.5,2,2.75,3.5]
        for off in kicks: note(drums,b0+off+human(rng,.006),.09,36,clamp(dv+(8 if off in (0,2) else -8),1,127),9)
        for off in (1,3): note(drums,b0+off+human(rng,.006),.10,38,clamp(dv+2,1,127),9)
        if bar%4==0 and i>=5: note(drums,b0,.18,49,72+int(20*intensity),9)
        if bar in (7,15,23):
            for j,n in enumerate([45,47,48,50]): note(drums,b0+3+j*.23,.10,n,70+int(18*intensity)+j*2,9)

    sc=[n+12 for n in scale(root,mode)]
    rhy=RHY[(i-1)%len(RHY)]; contour=CON[(i-1)%len(CON)]
    last=root+24
    for ps in range(0,24,4):
        if i<=3 and ps in (4,12,20): continue
        for lb in range(4):
            bar=ps+lb; b0=bar*4
            use=rhy if lb%2==0 else rhy[:max(3,len(rhy)-1)]
            for k,off in enumerate(use):
                raw=sc[contour[(k+lb+ps//4)%len(contour)]%7]
                if 12<=bar<18: raw += 5 if mode=="minor" else 7
                candidates=[x for x in (raw-12,raw,raw+12) if 52<=x<=92]
                p=min(candidates,key=lambda x:abs(x-last))
                if k==0 and bar%8==0 and i>=8:p=min(91,p+12)
                dur=.42 if k<len(use)-1 else .68
                if i>=14:dur*=.86
                note(lead,b0+off+human(rng,.010),dur,p,clamp(lv+rng.randint(-5,5),1,127),0)
                last=p

    # assinatura de metais diferente em cada onda
    sig=(8+((i-1)%4))*4
    sigdeg=[0,4,3,4,0] if mode=="major" else [0,6,5,6,0]
    for k,d in enumerate(sigdeg):
        rr,ch=triad(hr,d,mode)
        chord(brass,sig+k*.72,.48,[n+24 for n in ch],clamp(bv+12,1,127),4,.005)

    tracks=[
        track("Guitarra Solo",0,30,76,110,lead),
        track("Guitarra Base L",1,29,32,102,g1),
        track("Guitarra Base R",2,29,96,102,g2),
        track("Baixo",3,33,60,108,bass),
        track("Metais",4,61,66,100,brass),
        track("Cordas",5,48,64,90,strings),
        track("Bateria GM",9,None,64,110,drums),
    ]
    path=MID/f"onda_{i:02d}.mid"
    save(path,bpm,f"Onda {i:02d} - {title}",tracks)

for i,s in enumerate(SPECS,1): compose(i,*s)

bat=r'''@echo off
setlocal EnableExtensions EnableDelayedExpansion

REM AJUSTE SOMENTE ESTA LINHA:
set "SF2=C:\SoundFonts\Timbres Of Heaven GM_GS_XG_SFX V 3.4 Final.sf2"

set "MIDI_DIR=%~dp0mid"
set "WAV_DIR=%~dp0wav_temp"
set "OGG_DIR=%~dp0ogg"

if not exist "%WAV_DIR%" mkdir "%WAV_DIR%"
if not exist "%OGG_DIR%" mkdir "%OGG_DIR%"

where fluidsynth >nul 2>nul || (echo ERRO: FluidSynth nao foi encontrado no PATH.& pause & exit /b 1)
where ffmpeg >nul 2>nul || (echo ERRO: FFmpeg nao foi encontrado no PATH.& pause & exit /b 1)
if not exist "%SF2%" (echo ERRO: SoundFont nao encontrado: "%SF2%" & pause & exit /b 1)

for %%F in ("%MIDI_DIR%\onda_*.mid") do (
  echo Renderizando %%~nxF...
  fluidsynth -ni -g 0.70 -r 44100 -F "%WAV_DIR%\%%~nF.wav" "%SF2%" "%%F"
  if errorlevel 1 exit /b 1

  ffmpeg -y -hide_banner -loglevel warning ^
    -i "%WAV_DIR%\%%~nF.wav" ^
    -af "loudnorm=I=-16:TP=-1.5:LRA=11" ^
    -c:a libvorbis -q:a 5 ^
    "%OGG_DIR%\%%~nF.ogg"
  if errorlevel 1 exit /b 1

  del "%WAV_DIR%\%%~nF.wav"
)

rmdir "%WAV_DIR%" 2>nul
echo.
echo Concluido. Os OGGs estao em:
echo "%OGG_DIR%"
pause
'''
(OUT/"renderizar_24_ondas.bat").write_text(bat,encoding="utf-8")

readme="""ASTEROIDE MATEMATICO - 24 MUSICAS ORIGINAIS

Arquivos: onda_01.mid a onda_24.mid
Linha musical: heroica / defesa urbana / marcha moderna.
As ondas ficam gradualmente mais intensas.

Canais GM:
1  Guitarra Solo - Distortion Guitar
2  Guitarra Base L - Overdriven Guitar
3  Guitarra Base R - Overdriven Guitar
4  Baixo - Electric Bass (finger)
5  Metais - Brass Section
6  Cordas - String Ensemble 1
10 Bateria GM

Para converter tudo:
1. Edite renderizar_24_ondas.bat.
2. Ajuste a variavel SF2 para o caminho do Timbres Of Heaven.
3. FluidSynth e FFmpeg devem estar no PATH.
4. Execute o BAT.
5. Os OGGs serao criados em ogg/.
"""
(OUT/"LEIA-ME.txt").write_text(readme,encoding="utf-8")

for p in MID.glob("*.mid"):
    d=p.read_bytes()
    assert d[:4]==b"MThd"
    assert d.count(b"MTrk")==8

zip_path=OUT/"asteroide_matematico_24_ondas_mid.zip"
with zipfile.ZipFile(zip_path,"w",zipfile.ZIP_DEFLATED) as z:
    for p in sorted(OUT.rglob("*")):
        if p.is_file() and p!=zip_path:
            z.write(p,p.relative_to(OUT))

print("Gerados 24 MIDIs e:",zip_path)
