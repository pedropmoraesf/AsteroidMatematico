from pathlib import Path
import struct, zipfile, math, random, shutil

ROOT=Path("generated_music")
MID=ROOT/"mid"
DIST=Path("dist")
TPB=480

def vlq(n):
    n=max(0,int(n)); out=[n&127]
    n >>= 7
    while n:
        out.append((n&127)|128); n >>= 7
    return bytes(reversed(out))

def msg(delta,status,d1=None,d2=None):
    b=bytearray(vlq(delta)); b.append(status&255)
    if d1 is not None: b.append(d1&127)
    if d2 is not None: b.append(d2&127)
    return bytes(b)

def meta(delta,typ,data=b""):
    return vlq(delta)+bytes([255,typ])+vlq(len(data))+data

def add_note(ev,beat,dur,note,vel,ch):
    st=round(beat*TPB); en=round((beat+max(.04,dur))*TPB)
    ev.append((st,2,lambda d,n=note,v=vel,c=ch:msg(d,0x90|c,n,v)))
    ev.append((en,1,lambda d,n=note,c=ch:msg(d,0x80|c,n,0)))

def add_chord(ev,beat,dur,notes,vel,ch,spread=.0):
    for i,n in enumerate(notes):
        add_note(ev,beat+i*spread,dur,n,max(1,vel-i*2),ch)

def make_track(name,ch=None,program=None,pan=64,vol=100,ev=None):
    items=[(0,0,lambda d,n=name:meta(d,3,n.encode("utf-8")))]
    if ch is not None:
        if ch!=9 and program is not None:
            items.append((0,0,lambda d,c=ch,p=program:msg(d,0xC0|c,p)))
        items += [
            (0,0,lambda d,c=ch,v=pan:msg(d,0xB0|c,10,v)),
            (0,0,lambda d,c=ch,v=vol:msg(d,0xB0|c,7,v)),
            (0,0,lambda d,c=ch:msg(d,0xB0|c,91,34 if c!=9 else 12))
        ]
    items.extend(ev or [])
    items.sort(key=lambda x:(x[0],x[1]))
    data=bytearray(); last=0
    for tick,_,fn in items:
        data.extend(fn(tick-last)); last=tick
    data.extend(meta(0,0x2F,b""))
    return b"MTrk"+struct.pack(">I",len(data))+data

def tempo_track(bpm,title):
    us=round(60000000/bpm)
    items=[
        (0,0,lambda d,t=title:meta(d,3,t.encode("utf-8"))),
        (0,0,lambda d:meta(d,2,b"Original para Asteroide Matematico")),
        (0,0,lambda d,u=us:meta(d,0x51,u.to_bytes(3,"big"))),
        (0,0,lambda d:meta(d,0x58,bytes([4,2,24,8])))
    ]
    data=bytearray(); last=0
    for tick,_,fn in items:
        data.extend(fn(tick-last)); last=tick
    data.extend(meta(0,0x2F,b""))
    return b"MTrk"+struct.pack(">I",len(data))+data

def write_midi(path,bpm,title,tracks):
    chunks=[tempo_track(bpm,title)]+tracks
    path.write_bytes(b"MThd"+struct.pack(">IHHH",6,1,len(chunks),TPB)+b"".join(chunks))

ROOTS={"C":60,"C#":61,"D":62,"Eb":63,"E":64,"F":65,"F#":66,"G":67,"Ab":68,"A":69,"Bb":70,"B":71}
MINOR=[0,2,3,5,7,8,10]
MAJOR=[0,2,4,5,7,9,11]
PROG_M=[[0,5,2,6],[0,6,5,6],[0,3,6,0],[0,2,5,6],[0,5,3,6],[0,6,2,5]]
PROG_J=[[0,4,5,3],[0,3,4,0],[5,3,0,4],[0,5,3,4],[3,0,4,5],[0,2,3,4]]

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

def chord(root,degree,mode):
    scale=MINOR if mode=="minor" else MAJOR
    qualities=(["m","dim","M","m","m","M","M"] if mode=="minor" else ["M","m","m","M","M","m","dim"])
    r=root+scale[degree]; q=qualities[degree]
    ints=[0,4,7] if q=="M" else ([0,3,7] if q=="m" else [0,3,6])
    return r,[r+i for i in ints]

def h(rng,a=.018): return rng.uniform(-a,a)

def make_wave(idx,root_name,mode,bpm,title,out_no):
    rng=random.Random(1000+idx*7919)
    root=ROOTS[root_name]; hr=root-12
    prog=(PROG_M if mode=="minor" else PROG_J)[(idx-1)%6]
    prog2=(PROG_M if mode=="minor" else PROG_J)[(idx+1)%6]
    lead=[]; gl=[]; gr=[]; bass=[]; brass=[]; strings=[]; drums=[]
    inten=(idx-1)/23.0
    bars=[]
    for b in range(32):
        pr=prog2 if 16<=b<24 else prog
        bars.append(chord(hr,pr[b%4],mode))
    for b,(rr,ch) in enumerate(bars):
        b0=b*4
        add_chord(strings,b0,3.9,[n+12 for n in ch],42+int(14*inten),5,.012)
        brass_off=(0,2) if idx<12 else (0,1.5,2.75)
        for off in brass_off:
            add_chord(brass,b0+off,.55,[n+24 for n in ch],50+int(28*inten),4,.006)
        patt=[0,2] if idx<=4 else ([0,1,2,3] if idx<=10 else [0,.5,1,1.5,2,2.5,3,3.5])
        dur=.68 if idx<=4 else (.48 if idx<=10 else .28)
        for k,off in enumerate(patt):
            notes=[rr-12,rr-5,rr]
            add_chord(gl,b0+off+h(rng),dur,notes,56+int(22*inten)+(6 if off in (0,2) else 0),1,.004)
            add_chord(gr,b0+off+.018+h(rng,.01),dur*.94,notes,51+int(22*inten),2,.004)
        bp=[0,1,2,3] if idx<8 else [0,.5,1,1.5,2,2.5,3,3.5]
        for j,off in enumerate(bp):
            add_note(bass,b0+off+h(rng,.008),.44 if len(bp)>4 else .76,rr-24 if j%4!=3 else rr-17,78+int(14*inten),3)
        step=.5 if idx<13 else .25
        x=0
        while x<4:
            add_note(drums,b0+x+h(rng,.005),.04,42,48+int(8*inten)+(7 if abs(x-round(x))<.01 else 0),9); x+=step
        kicks=[0,2] if idx<5 else ([0,1.5,2,3.25] if idx<15 else [0,.75,1.5,2,2.75,3.5])
        for off in kicks: add_note(drums,b0+off,.08,36,84+int(18*inten),9)
        for off in (1,3): add_note(drums,b0+off,.10,38,88+int(15*inten),9)
        if b%4==0 and idx>=5: add_note(drums,b0,.18,49,76+int(18*inten),9)
        if b in (7,15,23,31):
            for j,n in enumerate((45,47,48,50)): add_note(drums,b0+3+j*.23,.10,n,74+j*3+int(14*inten),9)
    scale=[root+i+12 for i in (MINOR if mode=="minor" else MAJOR)]
    rhythm=[[0,.75,1.5,2.25,3],[0,.5,1,2,2.5,3.25],[0,1,1.5,2.5,3],[0,.5,1.5,2,3,3.5]][(idx-1)%4]
    contour=[[0,2,4,3,2,4],[0,4,5,4,2,1],[2,3,4,6,4,2],[0,1,3,4,5,3]][(idx-1)%4]
    last=root+24
    for b in range(32):
        if idx<=3 and b%8==4: continue
        for j,off in enumerate(rhythm):
            raw=scale[contour[(j+b)%len(contour)]%7]
            if 16<=b<24: raw += 5 if mode=="minor" else 7
            cand=[raw-12,raw,raw+12]
            pitch=min(cand,key=lambda n:abs(n-last))
            pitch=max(52,min(91,pitch))
            add_note(lead,b*4+off+h(rng,.01),.40 if j<len(rhythm)-1 else .68,pitch,90+int(16*inten)+rng.randint(-4,4),0)
            last=pitch
    tracks=[
        make_track("Guitarra Solo",0,30,76,110,lead),
        make_track("Guitarra Base L",1,29,32,102,gl),
        make_track("Guitarra Base R",2,29,96,102,gr),
        make_track("Baixo",3,33,60,108,bass),
        make_track("Metais",4,61,66,100,brass),
        make_track("Cordas",5,48,64,90,strings),
        make_track("Bateria GM",9,None,64,110,drums)
    ]
    write_midi(MID/f"onda_{out_no:02d}.mid",bpm,f"Onda {out_no:02d} - {title}",tracks)

def make_victory():
    bpm=132; root=62; rng=random.Random(250025)
    lead=[]; brass=[]; strings=[]; bass=[]; gl=[]; gr=[]; drums=[]
    # D major finale: I - V - vi - IV, then I - IV - V - I
    progress=[0,4,5,3,0,3,4,0]
    for b in range(24):
        degree=progress[b%len(progress)]
        rr,ch=chord(root-12,degree,"major")
        b0=b*4
        add_chord(strings,b0,3.9,[n+12 for n in ch],62,5,.01)
        for off in (0,2): add_chord(brass,b0+off,.8,[n+24 for n in ch],86,4,.006)
        for off in (0,.5,1,1.5,2,2.5,3,3.5):
            pw=[rr-12,rr-5,rr]
            add_chord(gl,b0+off,.34,pw,76,1,.004)
            add_chord(gr,b0+off+.016,.32,pw,72,2,.004)
            add_note(bass,b0+off,.38,rr-24 if int(off*2)%4!=3 else rr-17,94,3)
            add_note(drums,b0+off,.04,42,62,9)
        for off in (0,1.5,2,3.25): add_note(drums,b0+off,.08,36,102,9)
        for off in (1,3): add_note(drums,b0+off,.10,38,105,9)
        if b%2==0: add_note(drums,b0,.18,49,100,9)
    scale=[root+i+12 for i in MAJOR]
    melody=[0,2,4,6,4,2,1,0, 4,5,6,4,2,3,4,6]
    for b in range(24):
        for j,off in enumerate((0,.5,1.25,2,2.75,3.5)):
            pitch=scale[melody[(b*2+j)%len(melody)]%7]
            if b>=16: pitch+=12 if j in (0,5) else 0
            add_note(lead,b*4+off+h(rng,.008),.38 if j<5 else .72,min(94,pitch),106,0)
    # final sustained D major
    add_chord(brass,96,3.8,[74,78,81,86],112,4,.015)
    add_chord(strings,96,3.8,[62,66,69,74],92,5,.015)
    add_chord(gl,96,3.8,[50,57,62],100,1,.008)
    add_chord(gr,96.02,3.8,[50,57,62],96,2,.008)
    for off in (0,.5,1,1.5,2,2.5,3,3.5):
        add_note(drums,96+off,.08,36 if off%1==0 else 42,106,9)
    tracks=[
        make_track("Guitarra Solo",0,30,76,112,lead),
        make_track("Guitarra Base L",1,29,32,104,gl),
        make_track("Guitarra Base R",2,29,96,104,gr),
        make_track("Baixo",3,33,60,110,bass),
        make_track("Metais",4,61,66,108,brass),
        make_track("Cordas",5,48,64,98,strings),
        make_track("Bateria GM",9,None,64,112,drums)
    ]
    write_midi(MID/"vitoria_final.mid",bpm,"Vitoria Final - Asteroide Matematico",tracks)

def make_bat():
    bat=r'''@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"

set "SF2_NAME=Timbres Of Heaven GM_GS_XG_SFX V 3.4 Final.sf2"

set "FLUID=%~dp0fluidsynth.exe"
if not exist "%FLUID%" set "FLUID=%~dp0..\fluidsynth.exe"
if not exist "%FLUID%" set "FLUID=fluidsynth"

set "SF2=%~dp0%SF2_NAME%"
if not exist "%SF2%" set "SF2=%~dp0..\%SF2_NAME%"

set "FFMPEG=%~dp0ffmpeg.exe"
if not exist "%FFMPEG%" set "FFMPEG=%~dp0..\ffmpeg.exe"
if not exist "%FFMPEG%" set "FFMPEG=ffmpeg"

if not exist "%SF2%" (
  echo ERRO: SoundFont nao encontrado.
  echo Coloque "%SF2_NAME%" na mesma pasta deste BAT
  echo ou na pasta imediatamente acima.
  pause
  exit /b 1
)

where "%FLUID%" >nul 2>nul
if errorlevel 1 if not exist "%FLUID%" (
  echo ERRO: fluidsynth.exe nao encontrado.
  pause
  exit /b 1
)

where "%FFMPEG%" >nul 2>nul
if errorlevel 1 if not exist "%FFMPEG%" (
  echo ERRO: ffmpeg.exe nao encontrado.
  echo Coloque ffmpeg.exe nesta pasta, na pasta acima, ou no PATH.
  pause
  exit /b 1
)

if not exist "ogg" mkdir "ogg"
if not exist "wav_temp" mkdir "wav_temp"

echo.
echo Convertendo ondas 02 a 25 e musica de vitoria...
echo.

for %%F in ("mid\onda_*.mid" "mid\vitoria_final.mid") do (
  echo [MIDI] %%~nxF
  "%FLUID%" -ni -g 0.70 -r 44100 -F "wav_temp\%%~nF.wav" "%SF2%" "%%F"
  if errorlevel 1 (
    echo ERRO ao renderizar %%~nxF
    pause
    exit /b 1
  )

  "%FFMPEG%" -y -hide_banner -loglevel warning ^
    -i "wav_temp\%%~nF.wav" ^
    -af "loudnorm=I=-16:TP=-1.5:LRA=11" ^
    -c:a libvorbis -q:a 5 ^
    "ogg\%%~nF.ogg"

  if errorlevel 1 (
    echo ERRO ao converter %%~nxF
    pause
    exit /b 1
  )

  del "wav_temp\%%~nF.wav"
)

rmdir "wav_temp" 2>nul
echo.
echo CONCLUIDO.
echo OGGs em: "%~dp0ogg"
echo.
pause
'''
    (ROOT/"converter_todos_para_ogg.bat").write_text(bat,encoding="utf-8")

def main():
    if ROOT.exists(): shutil.rmtree(ROOT)
    MID.mkdir(parents=True)
    DIST.mkdir(exist_ok=True)
    for i,spec in enumerate(SPECS,1):
        make_wave(i,*spec,out_no=i+1)
    make_victory()
    make_bat()
    (ROOT/"LEIA-ME.txt").write_text(
"""ASTEROIDE MATEMATICO - MUSICAS DAS ONDAS 02 A 25 + VITORIA FINAL

Conteudo:
- mid/onda_02.mid ate mid/onda_25.mid
- mid/vitoria_final.mid
- converter_todos_para_ogg.bat

Como usar:
1. Extraia o conteudo na pasta onde esta fluidsynth.exe.
2. Deixe nessa mesma pasta o arquivo:
   Timbres Of Heaven GM_GS_XG_SFX V 3.4 Final.sf2
3. Deixe ffmpeg.exe na mesma pasta, na pasta acima, ou no PATH.
4. Execute converter_todos_para_ogg.bat.
5. Copie os arquivos criados em ogg/ para assets/audio/ do projeto.

O jogo usa musica_jogo.ogg na onda 01.
Depois procura onda_02.ogg ... onda_25.ogg e vitoria_final.ogg.
""",encoding="utf-8")
    out=DIST/"asteroide_matematico_ondas_02_25_mid.zip"
    if out.exists(): out.unlink()
    with zipfile.ZipFile(out,"w",zipfile.ZIP_DEFLATED) as z:
        for p in sorted(ROOT.rglob("*")):
            if p.is_file(): z.write(p,p.relative_to(ROOT))
    print(out, out.stat().st_size)

if __name__=="__main__":
    main()
