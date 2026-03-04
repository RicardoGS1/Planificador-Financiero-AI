package com.virtualworld.easyexpensecontrol.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.virtualworld.easyexpensecontrol.R

@Composable
fun MonthPicker(
    visible: Boolean,
    currentMonth: Int,
    currentYear: Int,
    confirmButtonCLicked: (Int, Int) -> Unit,
    cancelClicked: () -> Unit
) {
    val months = listOf(
        "ENE", "FEB", "MAR", "ABR", "MAY", "JUN",
        "JUL", "AGO", "SEP", "OCT", "NOV", "DIC"
    )

    var selectedMonthIndex by remember { mutableIntStateOf(currentMonth - 1) }
    var year by remember { mutableIntStateOf(currentYear) }

    if (visible) {
        AlertDialog(
            backgroundColor = colorResource(R.color.blue_white),
            shape = RoundedCornerShape(25.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp),
            title = {},
            text = {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { year-- },
                            modifier = Modifier.rotate(90f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(35.dp)
                            )
                        }

                        Text(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            text = year.toString(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(
                            onClick = { year++ },
                            modifier = Modifier.rotate(-90f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(35.dp)
                            )
                        }
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 30.dp)
                    ) {
                        items(months) { monthName ->
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clickable {
                                        selectedMonthIndex = months.indexOf(monthName)
                                    }
                                    .background(
                                        color = if (selectedMonthIndex == months.indexOf(monthName)) colorResource(R.color.blue_dark) else colorResource(R.color.blue_white),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = monthName,
                                    color = if (selectedMonthIndex == months.indexOf(monthName)) colorResource(R.color.blue_white) else colorResource(R.color.bold_from_palette),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            },
            buttons = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 20.dp, bottom = 20.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = { cancelClicked() },
                        shape = CircleShape,
                        border = BorderStroke(1.dp, color = colorResource(R.color.blue_transparent)),
                        colors = ButtonDefaults.outlinedButtonColors(backgroundColor = colorResource(R.color.blue_transparent)),
                        modifier = Modifier.padding(end = 20.dp)
                    ) {
                        Text(
                            text = "Cancelar",
                            color = colorResource(R.color.bold_from_palette),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            confirmButtonCLicked(selectedMonthIndex + 1, year)
                        },
                        shape = CircleShape,
                        border = BorderStroke(1.dp, color = colorResource(R.color.blue_dark)),
                        colors = ButtonDefaults.outlinedButtonColors(backgroundColor = colorResource(R.color.blue_transparent)),
                        modifier = Modifier.padding(end = 20.dp)
                    ) {
                        Text(
                            text = "OK",
                            color = colorResource(R.color.blue_dark),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            onDismissRequest = {}
        )
    }
}
