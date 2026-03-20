# 🔥 AniForge — Rama `ParaLaAPK`

Port completo del proyecto Python → Java/Android para Fire TV.

---

## 🚀 Cómo crear la rama y subir este código

```bash
# 1. Clonar tu repositorio original
git clone https://github.com/Jesus2421/AniForge.git
cd AniForge

# 2. Crear la rama nueva
git checkout -b ParaLaAPK

# 3. Copiar TODO el contenido de esta carpeta AniForge-Android/ dentro del repo
#    (la carpeta app/, build.gradle, settings.gradle, gradle.properties, etc.)

# 4. Añadir, commitear y subir
git add .
git commit -m "feat: port completo Python → Java/Android para Fire TV"
git push origin ParaLaAPK
```

---

## 🛠️ Requisitos para compilar

- **Android Studio** Hedgehog 2023.1.1 o superior
- **JDK 11** (incluido en Android Studio)
- **Android SDK** API 34
- Conexión a internet (descarga dependencias de Gradle la primera vez)

### Pasos en Android Studio

1. `File → Open` → selecciona esta carpeta raíz
2. Espera a que Gradle sincronice (primera vez tarda 2-5 min)
3. Conecta tu Fire TV por ADB o usa un emulador TV
4. `Run → Run 'app'`

### Habilitar ADB en Fire TV

```
Ajustes → Mi Fire TV → Opciones de desarrollador
→ Activar "Depuración ADB"
→ Activar "Aplicaciones de origen desconocido"
```

```bash
# Conectar Fire TV por red (misma WiFi)
adb connect <IP_DEL_FIRE_TV>:5555
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 📁 Estructura del proyecto

```
app/src/main/
├── AndroidManifest.xml          ← Permisos, actividades, leanback launcher
├── java/com/aniforge/
│   ├── api/
│   │   ├── AnimeFLVClient.java  ← Port de animeflv.py (OkHttp + Jsoup)
│   │   └── AnimeFLVParseException.java  ← Port de exception.py
│   ├── model/
│   │   ├── AnimeInfo.java       ← Port del dataclass AnimeInfo
│   │   ├── EpisodeInfo.java     ← Port del dataclass EpisodeInfo
│   │   └── VideoServer.java     ← Port de DownloadLinkInfo
│   └── ui/
│       ├── home/
│       │   ├── MainActivity.java       ← Pantalla principal
│       │   ├── MainViewModel.java      ← Lógica async (reemplaza main.py)
│       │   └── AnimeCardAdapter.java   ← Grid de animes
│       ├── detail/
│       │   ├── DetailActivity.java     ← Detalle + lista de episodios
│       │   └── EpisodeAdapter.java
│       ├── search/
│       │   └── SearchActivity.java     ← Búsqueda con teclado Fire TV
│       └── player/
│           └── PlayerActivity.java     ← ExoPlayer con control remoto
└── res/
    ├── layout/                  ← Todos los XMLs de pantalla
    ├── values/
    │   ├── colors.xml           ← Paleta oscura (bg #0D0D0D, accent #FF4500)
    │   ├── strings.xml
    │   └── themes.xml           ← Tema NoActionBar (requerido para TV)
    ├── drawable/                ← Selectores de estado focused para D-pad
    └── xml/
        └── network_security_config.xml
```

---

## 🐛 Bugs del proyecto Python que se corrigieron aquí

| # | Bug en Python | Corrección en Java |
|---|---------------|-------------------|
| 1 | `ANIME_URL + "/" + id` generaba doble slash | Corregido a `ANIME_URL + id` |
| 2 | `AnimeFLVParseError` en `_process_anime_list_info` abortaba toda la lista | Ahora se loguea el error y se continúa con los demás elementos |
| 3 | Sin verificación del código HTTP de respuesta | `response.isSuccessful()` antes de leer el body |
| 4 | Sin fallback entre servidores de video | `tryNextServer()` automático en `PlayerActivity` |
| 5 | Sin manejo de errores de red en main.py | Cada llamada tiene try/catch + `errorMessage` LiveData |
| 6 | No había soporte D-pad (inutilizable en Fire TV) | Todos los elementos tienen `focusable`, selectores de estado y `onFocusChangeListener` |
| 7 | `venv/` commiteado en el repo | `.gitignore` debe incluir `venv/`, `dist/`, `build/`, `*.spec` |

---

## 📦 Dependencias clave

| Python (original) | Java/Android (equivalente) |
|-------------------|---------------------------|
| `cloudscraper` | `OkHttp 4.12` con headers de navegador |
| `BeautifulSoup` | `Jsoup 1.17` |
| `json` (stdlib) | `org.json` (incluido en Android) |
| `asyncio` | `ExecutorService` + `LiveData` |
| `tkinter` / consola | Activities + RecyclerView + Leanback |
| — | `ExoPlayer (Media3)` para reproducción |
| — | `Glide` para imágenes async |

---

## ⚠️ Pendiente (próximos pasos)

- [ ] Reemplazar `app_banner.xml` con un PNG real de **320×180dp** (requerido por Fire TV Store)
- [ ] Reemplazar `ic_launcher` con el logo de AniForge  
- [ ] Implementar **WebView** o extractor de iframe para servidores que no dan URL directa (Streamtape, etc.)
- [ ] Añadir **caché local** con Room para favoritos e historial
- [ ] Manejar el caso de Cloudflare challenge (algunos servidores lo activan)
- [ ] Firmar el APK para distribución (`Build → Generate Signed APK`)

---

## 📜 Licencia

MIT — igual que el proyecto original.
