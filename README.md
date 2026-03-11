# 💰 Easy Expense Control — Finanzas Personales

Aplicación Android para el control de finanzas personales: registro de ingresos y gastos, presupuestos por categoría y **análisis de comprobantes con IA**.

---

## 🤖 IA para comprobantes

Al **añadir un gasto**, puedes **tomar una foto del ticket o factura**. La app envía la imagen a **Google Gemini** y obtiene de forma automática:

- **Importe** del comprobante  
- **Descripción breve** de la compra  
- **Categoría sugerida**, eligiendo entre tus categorías de gasto guardadas en la app (o una lista por defecto si ninguna encaja)

Los campos del formulario se rellenan solos; solo tienes que revisar y guardar. La IA usa las categorías que ya tienes en la base de datos para mejorar la precisión.

*Configuración necesaria:* clave de API de Gemini en `local.properties` (ver [Cómo ejecutar](#-cómo-ejecutar-el-proyecto)). Detalles del flujo en **READMEIA.md**.

---

## 📱 Descripción

**Easy Expense Control** permite gestionar tus finanzas de forma sencilla: registrar transacciones (ingresos y gastos), asignar categorías, definir presupuestos mensuales por categoría y consultar el historial y estadísticas. Destaca la **foto de comprobante con IA** para registrar gastos en segundos.

---

## ✨ Funcionalidades

- **🤖 Análisis de comprobantes con IA (Gemini)**: toma una foto del ticket o factura al añadir un gasto; la IA extrae importe, descripción y sugiere categoría (usando las de tu BD).
- **Dashboard**: resumen de ingresos, gastos y balance; últimas transacciones y acceso rápido.
- **Transacciones**: agregar, editar y eliminar ingresos y gastos con categoría, fecha y descripción.
- **Presupuestos**: crear y editar presupuestos por categoría y mes.
- **Historial**: consultar y filtrar transacciones y presupuestos.
- **Estadísticas**: visualización de datos para analizar tu situación financiera.

---

## 🛠 Tecnologías

| Área | Tecnología |
|------|------------|
| **IA / visión** | [Google Gemini API](https://ai.google.dev/) (Gemini 1.5 Flash) para análisis de imágenes de comprobantes |
| **Red** | [Retrofit](https://square.github.io/retrofit/) + OkHttp + Gson para llamadas a la API de Gemini |
| **Lenguaje** | [Kotlin](https://kotlinlang.org/) |
| **UI** | [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3) |
| **Navegación** | [Navigation Compose](https://developer.android.com/jetpack/compose/navigation) |
| **Persistencia** | [Room](https://developer.android.com/training/data-storage/room) (SQLite) |
| **Inyección de dependencias** | [Koin](https://insert-koin.io/) |
| **Arquitectura** | Clean Architecture (capas data / domain / UI), MVVM |
| **Fechas** | [kotlinx-datetime](https://github.com/Kotlin/kotlinx-datetime) |
| **Build** | Gradle (Kotlin DSL), [Version Catalog](https://docs.gradle.org/current/userguide/platforms.html) (libs.versions.toml) |

### Versiones principales

- **compileSdk**: 35  
- **minSdk**: 24 | **targetSdk**: 34  
- **Kotlin**: 2.1.0  
- **Compose BOM**: 2024.12.01  
- **Room**: 2.6.1  
- **Koin**: 4.0.0  

---

## 📂 Estructura del proyecto

El proyecto sigue **Clean Architecture** y buenas prácticas en Android:

```
Finanzas_Personales/
├── app/
│   └── src/main/java/.../easyexpensecontrol/
│       ├── MainActivity.kt          # Punto de entrada (Compose).
│       ├── FinancialApp.kt          # Application: inicialización de Koin.
│       ├── core/                    # Utilidades (status bar, fechas, etc.).
│       ├── data/                    # Capa de datos.
│       │   ├── model/               # Entidades Room (Transaction, Category, Budget).
│       │   ├── local/               # FinancialDatabase, DAOs.
│       │   ├── remote/             # API Gemini, DTOs, ReceiptRemoteDataSource.
│       │   └── repository/         # Implementaciones de repositorios.
│       ├── domain/                  # Capa de dominio.
│       │   ├── model/              # ReceiptResult (resultado del análisis IA).
│       │   ├── repository/         # Interfaces de repositorios.
│       │   └── usecase/            # Casos de uso (transaction, category, budget, receipt).
│       ├── di/                      # Módulo Koin (AppModule.kt).
│       ├── viewmodel/               # ViewModels (Transaction, Budget, Category).
│       └── ui/
│           ├── theme/               # Tema, colores, tipografía.
│           ├── navigation/          # Rutas, NavHost, ítems de navegación.
│           ├── components/          # Componentes reutilizables (AppBar, campos, BottomBar, etc.).
│           └── screens/             # Pantallas (Dashboard, History, Budget, Statics, Add/Edit).
├── build.gradle.kts
├── gradle/
│   └── libs.versions.toml          # Catálogo de versiones.
├── settings.gradle.kts
└── README.md
```

---

## 📋 Requisitos

- [Android Studio](https://developer.android.com/studio) (recomendado Ladybug o superior).
- JDK 17 (compatible con la configuración del proyecto).
- Dispositivo o emulador con **API 24** o superior.
- **Para la función de IA**: clave de API de [Google AI Studio (Gemini)](https://aistudio.google.com/apikey). Añádela en `local.properties` (ver más abajo).

---

## 🚀 Cómo ejecutar el proyecto

1. Clona el repositorio:
   ```bash
   git clone https://github.com/RicardoGS1/Finanzas_Personales.git
   cd Finanzas_Personales
   ```
2. **(Opcional, para análisis de comprobantes con IA)** Crea o edita `local.properties` en la raíz del proyecto y añade tu clave de Gemini:
   ```properties
   GEMINI_API_KEY=tu_api_key_de_google_ai_studio
   ```
   No subas este archivo a control de versiones. Sin esta clave, el resto de la app funciona; solo fallará "Tomar foto" al añadir un gasto.
3. Abre el proyecto en **Android Studio**.
4. Sincroniza Gradle (File → Sync Project with Gradle Files).
5. Conecta un dispositivo o inicia un emulador y pulsa **Run** (▶️).

Para compilar por línea de comandos:

```bash
./gradlew assembleDebug
```

El APK de debug se generará en `app/build/outputs/apk/debug/`.

---

## 📄 Licencia

Este proyecto está bajo la licencia que se indique en el repositorio. Si no se especifica, queda a criterio del autor.

---

**Documentación adicional**

- **READMEIA.md**: guía paso a paso de la implementación de la IA para comprobantes (flujo, configuración, opciones).

*Desarrollado con Kotlin y Jetpack Compose.*
