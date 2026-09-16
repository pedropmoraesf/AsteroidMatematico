# Compilação

Projeto configurado para Android Studio 2022.1.1 Patch 1.

- Android Gradle Plugin: 7.4.2
- Gradle: 7.5.1
- Java: 11
- compileSdk/targetSdk: 28
- minSdk: 8

As dependências AndEngine e AndEnginePhysicsBox2DExtension são carregadas pelos submódulos em `third_party/`.

Para clonar com as dependências:

```bash
git clone --recurse-submodules https://github.com/pedropmoraesf/AsteroidMatematico.git
```

Build de debug:

```bash
gradle assembleDebug
```
