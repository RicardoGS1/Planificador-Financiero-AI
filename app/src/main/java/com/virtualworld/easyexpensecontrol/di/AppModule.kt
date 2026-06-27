package com.virtualworld.easyexpensecontrol.di

import androidx.room.Room
import com.virtualworld.easyexpensecontrol.BuildConfig
import com.virtualworld.easyexpensecontrol.data.local.BudgetListVisibilityRepository
import com.virtualworld.easyexpensecontrol.data.local.OnboardingTutorialRepository
import com.virtualworld.easyexpensecontrol.data.local.FinancialDatabase
import com.virtualworld.easyexpensecontrol.data.local.FinancialDatabaseCallback
import com.virtualworld.easyexpensecontrol.data.remote.GeminiApi
import com.virtualworld.easyexpensecontrol.data.remote.ReceiptRemoteDataSource
import com.virtualworld.easyexpensecontrol.data.repository.AccountRepository
import com.virtualworld.easyexpensecontrol.data.repository.BudgetRepository
import com.virtualworld.easyexpensecontrol.data.repository.CategoryRepository
import com.virtualworld.easyexpensecontrol.data.repository.ReceiptAnalysisRepositoryImpl
import com.virtualworld.easyexpensecontrol.data.repository.TransactionRepository
import com.virtualworld.easyexpensecontrol.domain.repository.AccountRepository as AccountRepositoryDomain
import com.virtualworld.easyexpensecontrol.domain.repository.BudgetRepository as BudgetRepositoryDomain
import com.virtualworld.easyexpensecontrol.domain.repository.CategoryRepository as CategoryRepositoryDomain
import com.virtualworld.easyexpensecontrol.domain.repository.ReceiptAnalysisRepository
import com.virtualworld.easyexpensecontrol.domain.repository.TransactionRepository as TransactionRepositoryDomain
import com.virtualworld.easyexpensecontrol.domain.usecase.account.AddAccountUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.account.GetAccountsUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.account.GetVisibleAccountsUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.account.UpdateAccountUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.budget.AddBudgetUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.budget.DeleteBudgetUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.budget.GetBudgetForCategoryMonthAndYearUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.budget.GetBudgetByIdUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.budget.GetBudgetsUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.budget.UpdateBudgetUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.category.GetCategoriesByTypeUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.category.GetCategoriesUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.category.GetCategoryByIdUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.category.GetCategoryByNameUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.category.SeedDefaultCategoriesUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.receipt.ProcessAudioUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.receipt.ProcessReceiptUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.receipt.ProcessSpreadsheetUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.transaction.DeleteTransactionUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.transaction.GetTransactionByIdUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.transaction.GetTransactionsByCategoryAndDateUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.transaction.GetTransactionsUseCase
import com.virtualworld.easyexpensecontrol.domain.usecase.transaction.SaveTransactionUseCase
import com.virtualworld.easyexpensecontrol.viewmodel.AccountViewModel
import com.virtualworld.easyexpensecontrol.viewmodel.BudgetViewModel
import com.virtualworld.easyexpensecontrol.viewmodel.CategoryViewModel
import com.virtualworld.easyexpensecontrol.viewmodel.TransactionViewModel
import com.virtualworld.easyexpensecontrol.analytics.AnalyticsManager
import com.google.gson.Gson
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import java.util.concurrent.TimeUnit
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val appModule = module {

    single {
        val appContext = androidContext()
        Room.databaseBuilder(
            appContext,
            FinancialDatabase::class.java,
            "financialapp.db"
        )
            .addMigrations(
                FinancialDatabase.MIGRATION_1_2,
                FinancialDatabase.MIGRATION_2_3,
                FinancialDatabase.MIGRATION_3_4,
                FinancialDatabase.MIGRATION_4_5
            )
            .addCallback(FinancialDatabaseCallback(appContext))
            .build()
    }

    single { get<FinancialDatabase>().transactionDao() }
    single { get<FinancialDatabase>().categoryDao() }
    single { get<FinancialDatabase>().budgetDao() }
    single { get<FinancialDatabase>().accountDao() }

    single { BudgetListVisibilityRepository(androidContext()) }
    single { OnboardingTutorialRepository(androidContext()) }

    single {
        AnalyticsManager(androidContext()).also { AnalyticsManager.bind(it) }
    }

    // Repositorios (implementaciones data que cumplen interfaces domain)
    single<TransactionRepositoryDomain> { TransactionRepository(get()) }
    single<CategoryRepositoryDomain> { CategoryRepository(get()) }
    single<BudgetRepositoryDomain> { BudgetRepository(get()) }
    single<AccountRepositoryDomain> { AccountRepository(get()) }

    // Casos de uso - Account
    single { GetAccountsUseCase(get()) }
    single { GetVisibleAccountsUseCase(get()) }
    single { AddAccountUseCase(get()) }
    single { UpdateAccountUseCase(get()) }

    // Casos de uso - Transaction
    single { GetTransactionsUseCase(get()) }
    single { GetTransactionByIdUseCase(get()) }
    single { GetTransactionsByCategoryAndDateUseCase(get()) }
    single { SaveTransactionUseCase(get(), get(), get()) }
    single { DeleteTransactionUseCase(get(), get()) }

    // Casos de uso - Category
    single { GetCategoriesUseCase(get()) }
    single { GetCategoryByIdUseCase(get()) }
    single { GetCategoryByNameUseCase(get()) }
    single { GetCategoriesByTypeUseCase(get()) }
    single { SeedDefaultCategoriesUseCase(get(), androidContext()) }

    // Red - Gemini / análisis de comprobantes (timeouts amplios: la IA puede tardar con imagen/audio)
    single {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()
    }
    single { Gson() }
    single {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/v1beta/")
            .client(get())
            .addConverterFactory(GsonConverterFactory.create(get()))
            .build()
    }
    single { get<Retrofit>().create(GeminiApi::class.java) }
    single { ReceiptRemoteDataSource(get(), BuildConfig.GEMINI_API_KEY, get(), androidContext()) }
    single<ReceiptAnalysisRepository> { ReceiptAnalysisRepositoryImpl(get()) }
    single { ProcessReceiptUseCase(get(), get(), get()) }
    single { ProcessAudioUseCase(get(), get(), get()) }
    single { ProcessSpreadsheetUseCase(get(), get(), get()) }

    // Casos de uso - Budget
    single { GetBudgetsUseCase(get()) }
    single { GetBudgetByIdUseCase(get()) }
    single { GetBudgetForCategoryMonthAndYearUseCase(get()) }
    single { AddBudgetUseCase(get()) }
    single { UpdateBudgetUseCase(get()) }
    single { DeleteBudgetUseCase(get()) }

    // ViewModels
    viewModel {
        TransactionViewModel(
            getTransactionsUseCase = get(),
            getTransactionByIdUseCase = get(),
            getTransactionsByCategoryAndDateUseCase = get(),
            saveTransactionUseCase = get(),
            deleteTransactionUseCase = get(),
            processReceiptUseCase = get(),
            processAudioUseCase = get(),
            processSpreadsheetUseCase = get(),
            getCategoryByNameUseCase = get(),
            getCategoriesByTypeUseCase = get(),
            getVisibleAccountsUseCase = get(),
            appContext = androidContext(),
            analyticsManager = get()
        )
    }
    viewModel {
        BudgetViewModel(
            getBudgetsUseCase = get(),
            getBudgetByIdUseCase = get(),
            getBudgetForCategoryMonthAndYearUseCase = get(),
            addBudgetUseCase = get(),
            updateBudgetUseCase = get(),
            deleteBudgetUseCase = get(),
            analyticsManager = get()
        )
    }
    viewModel {
        AccountViewModel(
            getAccountsUseCase = get(),
            getVisibleAccountsUseCase = get(),
            addAccountUseCase = get(),
            updateAccountUseCase = get(),
            accountRepository = get()
        )
    }
    viewModel {
        CategoryViewModel(
            getCategoriesUseCase = get(),
            getCategoryByIdUseCase = get(),
            getCategoryByNameUseCase = get(),
            getCategoriesByTypeUseCase = get()
        )
    }
}
