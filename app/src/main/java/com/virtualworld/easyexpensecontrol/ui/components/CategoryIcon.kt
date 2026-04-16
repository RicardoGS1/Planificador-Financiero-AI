package com.virtualworld.easyexpensecontrol.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.ElectricalServices
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.LocalGroceryStore
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.LocalPharmacy
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector

data class CategoryIconEntry(
    val key: String,
    val icon: ImageVector,
    val labelResKey: String
)

object CategoryIcons {

    val DEFAULT_ICON_KEY = "wallet"

    private val iconMap = linkedMapOf(
        "wallet" to Icons.Outlined.AccountBalanceWallet,
        "grocery" to Icons.Outlined.LocalGroceryStore,
        "restaurant" to Icons.Outlined.Restaurant,
        "coffee" to Icons.Outlined.Coffee,
        "shopping" to Icons.Outlined.ShoppingCart,
        "shopping_bag" to Icons.Outlined.ShoppingBag,
        "home" to Icons.Outlined.Home,
        "electricity" to Icons.Outlined.ElectricalServices,
        "water" to Icons.Outlined.WaterDrop,
        "phone" to Icons.Outlined.Phone,
        "car" to Icons.Outlined.DirectionsCar,
        "bus" to Icons.Outlined.DirectionsBus,
        "gas" to Icons.Outlined.LocalGasStation,
        "flight" to Icons.Outlined.Flight,
        "hospital" to Icons.Outlined.LocalHospital,
        "pharmacy" to Icons.Outlined.LocalPharmacy,
        "school" to Icons.Outlined.School,
        "fitness" to Icons.Outlined.FitnessCenter,
        "movie" to Icons.Outlined.Movie,
        "music" to Icons.Outlined.MusicNote,
        "pets" to Icons.Outlined.Pets,
        "subscriptions" to Icons.Outlined.Subscriptions,
    )

    val allEntries: List<CategoryIconEntry> = iconMap.map { (key, icon) ->
        CategoryIconEntry(key = key, icon = icon, labelResKey = key)
    }

    fun getIcon(iconName: String?): ImageVector =
        iconMap[iconName] ?: iconMap[DEFAULT_ICON_KEY]!!

    fun isValidKey(key: String?): Boolean = key != null && key in iconMap
}
