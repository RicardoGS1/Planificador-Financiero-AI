# Control de gastos



## Estructura del proyecto

El proyecto sigue una estructura por capas (Clean Architecture) y buenas prácticas de Android:

```
EasyExpenseControl/
├── app/                          # Módulo principal de la aplicación.
│   └── src/main/java/.../easyexpensecontrol/
│       ├── MainActivity.kt       # Punto de entrada.
│       ├── FinancialApp.kt       # Application class (Koin).
│       ├── core/                 # Utilidades y extensión.
│       │   ├── SetStatusBarColor.kt
│       │   └── util/
│       │       └── DateUtils.kt
│       ├── data/                 # Capa de datos.
│       │   ├── model/            # Entidades (Room).
│       │   │   ├── Transaction.kt, Category.kt, Budget.kt, TransactionType.kt
│       │   ├── local/            # Base de datos y DAOs.
│       │   │   ├── FinancialDatabase.kt
│       │   │   ├── TransactionDao.kt, CategoryDao.kt, BudgetDao.kt
│       │   └── repository/       # Implementación de repositorios.
│       │       ├── TransactionRepository.kt, CategoryRepository.kt, BudgetRepository.kt
│       ├── domain/               # Capa de dominio (casos de uso).
│       │   ├── repository/       # Interfaces de repositorios.
│       │   │   ├── TransactionRepository.kt, CategoryRepository.kt, BudgetRepository.kt
│       │   └── usecase/
│       │       ├── transaction/  # SaveTransaction, DeleteTransaction, GetTransactions, etc.
│       │       ├── category/     # GetCategories, GetCategoryById, GetCategoryByName, etc.
│       │       └── budget/       # GetBudgets, AddBudget, UpdateBudget, DeleteBudget, etc.
│       ├── di/                   # Inyección de dependencias (Koin).
│       │   └── AppModule.kt
│       ├── viewmodel/            # ViewModels (MVVM) — usan casos de uso.
│       │   ├── TransactionViewModel.kt, BudgetViewModel.kt, CategoryViewModel.kt
│       └── ui/
│           ├── theme/            # Tema, colores y tipografía.
│           ├── navigation/      # Navegación (Screen, NavHost, NavItem).
│           ├── components/       # Componentes reutilizables (AppBar, campos, pickers).
│           └── screens/         # Pantallas y vistas de detalle.
├── .gradle/                      # Archivos generados por Gradle.
├── .idea/                        # Configuración del proyecto para Android Studio.
├── build.gradle.kts              # Configuración principal de Gradle.
├── gradle/                       # Scripts y configuraciones de Gradle.
├── gradlew, gradlew.bat          # Wrappers para Gradle.
├── local.properties              # Configuración local específica.
├── settings.gradle.kts           # Configuración de los módulos del proyecto.
```

