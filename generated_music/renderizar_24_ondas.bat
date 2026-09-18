@echo off
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
