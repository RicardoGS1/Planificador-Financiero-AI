# 💰 Easy Expense Control — Finanzas Personales

Aplicación Android para el control de finanzas personales: registro de ingresos y gastos, presupuestos por categoría y visualización de estadísticas.

---

## 📱 Descripción

**Easy Expense Control** permite gestionar tus finanzas de forma sencilla: registrar transacciones (ingresos y gastos), asignar categorías, definir presupuestos mensuales por categoría y consultar el historial y estadísticas de tus movimientos.

---

## ✨ Funcionalidades

- **Dashboard**: resumen de ingresos, gastos y balance; últimas transacciones y acceso rápido.
- **Transacciones**: agregar, editar y eliminar ingresos y gastos con categoría, fecha y descripción.
- **Presupuestos**: crear y editar presupuestos por categoría y mes.
- **Historial**: consultar y filtrar transacciones y presupuestos.
- **Estadísticas**: visualización de datos para analizar tu situación financiera.
- **Navegación**: barra inferior con curva (Curved Bottom Bar) y navegación por pantallas.

---

## 🛠 Tecnologías

| Área | Tecnología |
|------|------------|
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
│       │   └── repository/         # Implementaciones de repositorios.
│       ├── domain/                  # Capa de dominio.
│       │   ├── repository/         # Interfaces de repositorios.
│       │   └── usecase/            # Casos de uso (transaction, category, budget).
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

---

## 🚀 Cómo ejecutar el proyecto

1. Clona el repositorio:
   ```bash
   git clone https://github.com/RicardoGS1/Finanzas_Personales.git
   cd Finanzas_Personales
   ```
2. Abre el proyecto en **Android Studio**.
3. Sincroniza Gradle (File → Sync Project with Gradle Files).
4. Conecta un dispositivo o inicia un emulador y pulsa **Run** (▶️).

Para compilar por línea de comandos:

```bash
./gradlew assembleDebug
```

El APK de debug se generará en `app/build/outputs/apk/debug/`.

---

## 📄 Licencia

Este proyecto está bajo la licencia que se indique en el repositorio. Si no se especifica, queda a criterio del autor.

---

*Desarrollado con Kotlin y Jetpack Compose.*
