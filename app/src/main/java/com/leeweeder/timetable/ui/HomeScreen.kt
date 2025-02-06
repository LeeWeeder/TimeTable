package com.leeweeder.timetable.ui

import android.annotation.SuppressLint
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.material.color.utilities.Scheme
import com.leeweeder.timetable.R
import com.leeweeder.timetable.data.source.SessionAndSubjectAndInstructor
import com.leeweeder.timetable.data.source.instructor.Instructor
import com.leeweeder.timetable.data.source.session.Session
import com.leeweeder.timetable.data.source.subject.Subject
import com.leeweeder.timetable.data.source.subject.SubjectWithInstructor
import com.leeweeder.timetable.data.source.timetable.TimeTable
import com.leeweeder.timetable.ui.components.EditScheduleDialog
import com.leeweeder.timetable.ui.components.IconButton
import com.leeweeder.timetable.ui.components.NewSubjectDialog
import com.leeweeder.timetable.ui.components.rememberSubjectDialogState
import com.leeweeder.timetable.ui.timetable_setup.LabelText
import com.leeweeder.timetable.ui.timetable_setup.components.TextButton
import com.leeweeder.timetable.ui.util.Constants
import com.leeweeder.timetable.ui.util.plusOneHour
import com.leeweeder.timetable.util.createScheme
import com.leeweeder.timetable.util.toColor
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale
import kotlin.collections.component1
import kotlin.collections.component2

@Composable
fun HomeScreen(
    onNavigateToSubjectsScreen: () -> Unit,
    viewModel: HomeViewModel = koinViewModel()
) {
    val dataState by viewModel.dataState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val uiEvent by viewModel.eventFlow.collectAsStateWithLifecycle()

    if (dataState is HomeDataState.Error) {
        Log.e("HomeScreen", "DataState error", (dataState as HomeDataState.Error).throwable)
    } else if (dataState is HomeDataState.Loading) {
        Log.d("HomeScreen", "Loading...")
    }

    HomeScreen(
        dataState = dataState,
        uiState = uiState,
        uiEvent = uiEvent,
        onEvent = viewModel::onEvent,
        onNavigateToSubjectsScreen = onNavigateToSubjectsScreen
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HomeScreen(
    dataState: HomeDataState,
    uiState: HomeUiState,
    uiEvent: HomeUiEvent?,
    onNavigateToSubjectsScreen: () -> Unit,
    onEvent: (HomeEvent) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val instructors by remember(dataState) {
        derivedStateOf {
            if (dataState is HomeDataState.Success) {
                dataState.instructors
            } else {
                emptyList()
            }
        }
    }

    val newSubjectDialogState = rememberSubjectDialogState(false)

    NewSubjectDialog(
        state = newSubjectDialogState,
        onConfirmClick = { subject, instructor ->
            onEvent(
                HomeEvent.SaveSubject(
                    newSubject = subject, instructor = instructor
                )
            )
        },
        instructors = instructors
    )

    val editSubjectDialogState = rememberSubjectDialogState(false)

    EditScheduleDialog(
        state = editSubjectDialogState,
        onConfirmClick = { subject, instructor ->
            onEvent(
                HomeEvent.SaveEditedSubject(
                    newSubject = subject, instructor = instructor
                )
            )
        },
        onDeleteSubjectClick = { subject, sessions ->
            onEvent(HomeEvent.DeleteSubject(subject, sessions))
            editSubjectDialogState.hide()
        },
        onScheduleClick = {
            onEvent(HomeEvent.SetActiveSubjectIdForEditing(it))
            onEvent(HomeEvent.SetOnEditMode)
            editSubjectDialogState.hide()
        },
        instructors = instructors
    )

    val snackBarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiEvent) {
        when (uiEvent) {
            HomeUiEvent.DoneEditingSubject -> {
                editSubjectDialogState.hide()
            }

            null -> Unit
            is HomeUiEvent.FinishedLoadingToBeEditedSubject -> {
                val subjectWithDetails = uiEvent.subjectWithDetails

                if (subjectWithDetails == null) {
                    return@LaunchedEffect
                }

                val subject = subjectWithDetails.subject
                editSubjectDialogState.init(
                    id = subject.id,
                    code = subject.code,
                    description = subject.description,
                    color = subject.color.toColor(),
                    instructor = subjectWithDetails.instructor ?: Instructor(name = ""),
                    sessions = subjectWithDetails.sessions
                )
                editSubjectDialogState.show()
            }

            HomeUiEvent.DoneAddingSubject -> {
                newSubjectDialogState.hide()
            }

            is HomeUiEvent.FinishedDeletingSubject -> {
                val subject = uiEvent.deletedSubject
                val result = snackBarHostState.showSnackbar(
                    message = subject.code + " deleted",
                    actionLabel = "Undo",
                    duration = SnackbarDuration.Long
                )

                if (result == SnackbarResult.ActionPerformed) {
                    onEvent(HomeEvent.ReinsertSubject(subject))
                }
            }
        }

        onEvent(HomeEvent.ClearUiEvent)
    }

    ModalNavigationDrawer(
        drawerContent = {
            TimeTableNavigationDrawer(
                drawerState = drawerState,
                selectedTimeTable = uiState.selectedTimeTable,
                onTimeTableClick = { timeTableId ->
                    onEvent(HomeEvent.SelectTimeTable(timeTableId))
                },
                onRecentSubjectClick = {
                    onEvent(HomeEvent.LoadToEditSubject(it))
                    scope.launch {
                        drawerState.close()
                    }
                },
                onNavigateToSubjectsScreen = onNavigateToSubjectsScreen,
                dataState = dataState
            )
        }, drawerState = drawerState
    ) {
        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackBarHostState)
            },
            topBar = {
                TopAppBar(title = uiState.selectedTimeTable.name,
                    topAppBarMode = if (uiState.isOnEditMode) {
                        TopAppBarMode.EditMode(onDoneClick = {
                            onEvent(HomeEvent.SetOnDefaultMode)
                        })
                    } else {
                        TopAppBarMode.Default(
                            onAddNewScheduleClick = {
                                newSubjectDialogState.reset()
                                newSubjectDialogState.show()
                            }, onMoreOptionsClick = {
                                // TODO: Implement more options
                            }
                        )
                    },
                    onNavigationMenuClick = {
                        scope.launch {
                            drawerState.open()
                        }
                    }
                )
            }
        ) { it ->
            Column(modifier = Modifier.padding(it)) {
                val leaderColumnWidth = 56.dp

                // Header
                LabelContainer(
                    modifier = Modifier.height(32.dp)
                ) {
                    Row {
                        Row {
                            Box(
                                modifier = Modifier.width(leaderColumnWidth)
                            )
                            CellBorder(CellBorderDirection.Vertical)
                        }

                        uiState.days.forEach { dayOfWeek ->
                            val backgroundColor = if (dayOfWeek == LocalDate.now().dayOfWeek) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            } else {
                                Color.Transparent
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(color = backgroundColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    dayOfWeek.getDisplayName(
                                        TextStyle.SHORT_STANDALONE, Locale.getDefault()
                                    ),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal)
                                )
                            }
                        }
                    }
                }

                // Body
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 24.dp)
                ) {
                    // Leader
                    LabelContainer(modifier = Modifier.width(leaderColumnWidth)) {
                        Column(modifier = Modifier.weight(1f)) {
                            val startTimes = uiState.startTimes
                            CellBorder(borderDirection = CellBorderDirection.Horizontal)
                            startTimes.forEachIndexed { index, period ->
                                Row(
                                    modifier = Modifier.height(RowHeight),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val style = MaterialTheme.typography.bodySmallEmphasized

                                    @Composable
                                    fun TimeText(time: LocalTime) {
                                        Text(
                                            time.format(Constants.TimeFormatter), style = style
                                        )
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        TimeText(period)
                                        Box(
                                            modifier = Modifier
                                                .size(width = 4.dp, height = 3.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(1.dp)
                                                    .background(color = MaterialTheme.colorScheme.onSurface)
                                            )
                                        }
                                        TimeText(period.plusOneHour())
                                    }
                                    CellBorder(borderDirection = CellBorderDirection.Vertical)
                                }
                                CellBorder(borderDirection = CellBorderDirection.Horizontal)
                            }
                        }
                    }

                    if (dataState is HomeDataState.Success) {
                        val selectedTimeTableId = uiState.selectedTimeTable.id
                        Log.d(
                            "HomeScreen",
                            "Selected timetable id (variable): $selectedTimeTableId"
                        )
                        Log.d(
                            "HomeScreen",
                            "Selected timetable id (state): ${uiState.selectedTimeTable.id}"
                        )

                        if (uiState.isOnEditMode) {
                            EditModeGrid(
                                dataState.getSessionsWithSubjectInstructor(
                                    selectedTimeTableId
                                ), onGridClick = {
                                    onEvent(HomeEvent.SetSessionWithSubject(it))
                                })
                        } else {
                            DefaultModeGrid(
                                dataState.getDayScheduleMap(selectedTimeTableId),
                                onChangeToEditMode = {
                                    onEvent(HomeEvent.LoadToEditSubject(it))
                                }
                            )
                        }

                        Log.d(
                            "HomeScreen",
                            "Selected timetable sessions (variable): ${
                                dataState.getSessionsWithSubjectInstructor(selectedTimeTableId)
                            }"
                        )
                        Log.d(
                            "HomeScreen",
                            "Selected timetable sessions (state): ${
                                dataState.getSessionsWithSubjectInstructor(uiState.selectedTimeTable.id)
                            }"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeTableNavigationDrawer(
    drawerState: DrawerState,
    selectedTimeTable: TimeTable,
    onTimeTableClick: (Int) -> Unit,
    onRecentSubjectClick: (Int) -> Unit,
    onNavigateToSubjectsScreen: () -> Unit,
    dataState: HomeDataState
) {

    @Composable
    fun IconToggleButton(
        checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier
    ) {
        var iconSize by remember { mutableStateOf(Size.Zero) }
        IconToggleButton(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .then(modifier)
                .onGloballyPositioned {
                    iconSize = it.size.toSize()
                },
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = IconButtonDefaults.iconToggleButtonColors(
                checkedContainerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    0.1f
                )
            )
        ) {
            val size = (iconSize / 2f)
            val density = LocalDensity.current
            Icon(
                painter = painterResource(R.drawable.more_vert_24px),
                contentDescription = "More options",
                modifier = Modifier.size(with(density) {
                    DpSize(size.width.toDp(), size.height.toDp())
                })
            )
        }
    }

    ModalDrawerSheet(
        drawerState = drawerState, drawerShape = RectangleShape
    ) {
        if (dataState is HomeDataState.Success) {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Unitimetable - University Timetable",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(thickness = Dp.Hairline)
                LazyColumn {
                    item {
                        Box(modifier = Modifier.padding(bottom = 8.dp, start = 16.dp, top = 8.dp)) {
                            LabelText("Time tables")
                        }
                    }
                    items(dataState.timeTables) { timeTable ->
                        val selected = timeTable == selectedTimeTable

                        Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                            NavigationDrawerItem(label = {
                                Text(timeTable.name)
                            }, selected = selected, icon = {
                                Icon(
                                    painter = painterResource(if (selected) R.drawable.table_24px else R.drawable.table_24px_outlined),
                                    contentDescription = null
                                )
                            }, onClick = {
                                onTimeTableClick(timeTable.id)
                            }, badge = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.offset(x = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    var isMainTable by remember { mutableStateOf(false) }

                                    isMainTable = dataState.mainTimeTable == timeTable
                                    if (isMainTable) {
                                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                            Text("Main")
                                        }
                                    }

                                    Box {
                                        var expanded by remember { mutableStateOf(false) }

                                        IconToggleButton(
                                            checked = expanded, onCheckedChange = {
                                                expanded = it
                                            }, modifier = Modifier.size(36.dp)
                                        )

                                        DropdownMenu(expanded = expanded, onDismissRequest = {
                                            expanded = false
                                        }) {
                                            DropdownMenuItem(text = {
                                                Text("Set as main timetable")
                                            }, enabled = !isMainTable, onClick = {
                                                // TODO: Implement onEvent for setting mainTableId data store
                                            }, trailingIcon = {
                                                if (isMainTable) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.done_24px),
                                                        contentDescription = null
                                                    )
                                                }
                                            })
                                            HorizontalDivider(
                                                thickness = Dp.Hairline,
                                                color = MaterialTheme.colorScheme.outlineVariant
                                            )
                                            DropdownMenuItem(text = {
                                                Text("Rename")
                                            }, onClick = {
                                                // TODO: Implement onEvent in renaming current timetable
                                            }, leadingIcon = {
                                                Icon(
                                                    painter = painterResource(R.drawable.text_format_24px),
                                                    contentDescription = null
                                                )
                                            })
                                            DropdownMenuItem(text = {
                                                Text("Edit layout")
                                            }, onClick = {
                                                // TODO: Implement onEvent in editing the layout of current timetable
                                            }, leadingIcon = {
                                                Icon(
                                                    painter = painterResource(R.drawable.table_edit_24px),
                                                    contentDescription = null
                                                )
                                            })
                                            DropdownMenuItem(text = {
                                                Text("Delete")
                                            }, onClick = {
                                                // TODO: Implement onEvent in deleting current timetable
                                            }, leadingIcon = {
                                                Icon(
                                                    painter = painterResource(R.drawable.delete_24px),
                                                    contentDescription = null
                                                )
                                            })
                                        }
                                    }
                                }
                            })
                        }
                    }
                    val fiveRecentlyAddedSubjects = dataState.fiveRecentlyAddedSubjects
                    if (fiveRecentlyAddedSubjects.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .padding(top = 24.dp, bottom = 8.dp)
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                HorizontalDivider(thickness = Dp.Hairline)
                                LabelText("Recently added subjects")
                            }
                        }
                        items(fiveRecentlyAddedSubjects) { subject ->
                            ListItem(
                                headlineContent = {
                                    Text(
                                        subject.description,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable(onClick = {
                                    onRecentSubjectClick(subject.id)
                                })
                            )
                        }
                        item {
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                OutlinedButton(
                                    onClick = onNavigateToSubjectsScreen,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("See all")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

sealed interface TopAppBarMode {
    data class Default(
        val onAddNewScheduleClick: () -> Unit,
        val onMoreOptionsClick: () -> Unit
    ) : TopAppBarMode

    data class EditMode(val onDoneClick: () -> Unit) : TopAppBarMode
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopAppBar(
    title: String, topAppBarMode: TopAppBarMode, onNavigationMenuClick: () -> Unit
) {
    TopAppBar(title = {
        Text(text = title)
    }, actions = {
        AnimatedContent(topAppBarMode) { topAppBarMode ->
            when (topAppBarMode) {
                is TopAppBarMode.Default -> {
                    Row {
                        IconButton(
                            R.drawable.add_24px,
                            contentDescription = "Add new schedule",
                            onClick = topAppBarMode.onAddNewScheduleClick
                        )
                        Box {
                            var expanded by remember { mutableStateOf(false) }

                            IconButton(
                                R.drawable.more_vert_24px,
                                contentDescription = "Open more options menu",
                                onClick = {
                                    expanded = true
                                }
                            )
                            DropdownMenu(expanded = expanded, onDismissRequest = {
                                expanded = false
                            }) {

                            }
                        }
                    }
                }

                is TopAppBarMode.EditMode -> {
                    IconButton(
                        R.drawable.done_24px,
                        contentDescription = "Finish scheduling",
                        onClick = topAppBarMode.onDoneClick
                    )
                }
            }
        }
    }, navigationIcon = {
        IconButton(
            R.drawable.menu_24px, "Open navigation menu", onClick = onNavigationMenuClick
        )
    }, colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    )
    )
}

@Composable
private fun LabelContainer(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        shadowElevation = 1.dp, tonalElevation = 1.dp, modifier = modifier
    ) {
        content()
    }
}

@Composable
private fun CellBorder(borderDirection: CellBorderDirection) {
    val thickness = Dp.Hairline
    val color = MaterialTheme.colorScheme.outlineVariant

    when (borderDirection) {
        CellBorderDirection.Horizontal -> {
            HorizontalDivider(thickness = thickness, color = color)
        }

        CellBorderDirection.Vertical -> {
            VerticalDivider(thickness = thickness, color = color)
        }
    }
}

@SuppressLint("RestrictedApi")
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun RowScope.DefaultModeGrid(
    dayScheduleMap: Map<DayOfWeek, List<Schedule>>, onChangeToEditMode: (subjectId: Int) -> Unit
) {
    dayScheduleMap.forEach { (_, schedules) ->
        Column(modifier = Modifier.weight(1f)) {
            schedules.forEach { schedule ->

                val subjectDescriptionMaxLine = if (schedule.periodSpan == 1) 2 else Int.MAX_VALUE
                val instructorNameMaxLine = if (schedule.periodSpan == 1) 1 else Int.MAX_VALUE

                Column(
                    modifier = Modifier.height(RowHeight * schedule.periodSpan),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (schedule.subject != null) {
                        val argbColor = schedule.subject.color.toArgb()

                        val scheme = if (isSystemInDarkTheme()) {
                            Scheme.dark(argbColor)
                        } else {
                            Scheme.light(argbColor)
                        }

                        var isTextTruncated by remember { mutableStateOf(false) }

                        val state = rememberTooltipState(isPersistent = true)

                        @Composable
                        fun Chip(
                            @DrawableRes iconId: Int,
                            iconColor: Color = AssistChipDefaults.assistChipColors().leadingIconContentColor,
                            text: String
                        ) {
                            AssistChip(onClick = { }, label = {
                                Text(
                                    text, style = MaterialTheme.typography.bodySmall
                                )
                            }, leadingIcon = {
                                Icon(
                                    painter = painterResource(iconId),
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = iconColor
                                )
                            }, modifier = Modifier.height(24.dp)
                            )
                        }

                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                            state = state,
                            tooltip = @Composable {
                                RichTooltip(action = {
                                    TextButton("Edit", onClick = {
                                        onChangeToEditMode(schedule.subject.id)
                                    })
                                }, title = {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Chip(
                                            iconId = R.drawable.book_24px,
                                            text = schedule.subject.code
                                        )
                                        Text(
                                            schedule.subject.description,
                                            style = MaterialTheme.typography.bodyLargeEmphasized
                                        )
                                    }
                                }) {
                                    Chip(
                                        iconId = R.drawable.account_box_24px,
                                        text = schedule.subject.instructor?.name
                                            ?: "No Instructor",
                                        iconColor = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            val scope = rememberCoroutineScope()
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border()
                                    .background(color = scheme.primary.toColor())
                                    .padding(4.dp)
                                    .clickable(onClick = {
                                        if (isTextTruncated) {
                                            scope.launch {
                                                state.show()
                                            }
                                        } else {
                                            onChangeToEditMode(schedule.subject.id)
                                        }
                                    }),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // TODO: Utilize parent size to distribute position and sizing of the texts
                                Text(
                                    schedule.subject.code.uppercase(),
                                    style = MaterialTheme.typography.labelMediumEmphasized,
                                    color = scheme.onPrimary.toColor(),
                                    textAlign = TextAlign.Center
                                    // TODO: Implement auto-size for subject code
                                )

                                val bodySmall = MaterialTheme.typography.bodySmall
                                val bodySmallFontSizeValue = bodySmall.fontSize.value
                                Text(schedule.subject.description,
                                    style = bodySmall.copy(
                                        fontSize = (bodySmallFontSizeValue - 2).sp,
                                        lineHeight = (bodySmallFontSizeValue - 1).sp
                                    ),
                                    color = scheme.onPrimary.toColor(),
                                    maxLines = subjectDescriptionMaxLine,
                                    softWrap = true,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    onTextLayout = {
                                        isTextTruncated = it.hasVisualOverflow
                                    })
                                Text(schedule.subject.instructor?.name ?: "No instructor",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = scheme.onPrimary.toColor(),
                                    modifier = Modifier.padding(top = 4.dp),
                                    maxLines = instructorNameMaxLine,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    onTextLayout = {
                                        isTextTruncated = it.hasVisualOverflow
                                    })
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(schedule.label ?: "")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Modifier.border(color: Color = MaterialTheme.colorScheme.surface): Modifier {
    return this.then(
        Modifier.border(
            width = Dp.Hairline, color = color
        )
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@SuppressLint("RestrictedApi")
@Composable
private fun RowScope.EditModeGrid(
    sessionsWithSubjectAndInstructor: List<SessionAndSubjectAndInstructor>,
    onGridClick: (Session) -> Unit
) {
    sessionsWithSubjectAndInstructor.groupBy { it.session.dayOfWeek }
        .forEach { (_, sessionsWithSubjectAndInstructor) ->
            Column(modifier = Modifier.weight(1f)) {
                sessionsWithSubjectAndInstructor.forEach { sessionAndSubjectAndInstructor ->
                    Box(
                        modifier = Modifier
                            .height(RowHeight)
                            .fillMaxWidth()
                            .clickable(
                                onClick = {
                                    onGridClick(sessionAndSubjectAndInstructor.session)
                                }
                            )
                    ) {
                        if (sessionAndSubjectAndInstructor.session.isSubject) {
                            val scheme = createScheme(
                                sessionAndSubjectAndInstructor.subjectWithInstructor!!.subject.color.toColor(),
                                isSystemInDarkTheme()
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(
                                        width = Dp.Hairline,
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                    .background(color = scheme.primary.toColor()),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    sessionAndSubjectAndInstructor.subjectWithInstructor.subject.code.uppercase(),
                                    modifier = Modifier.padding(4.dp),
                                    color = scheme.onPrimary.toColor(),
                                    style = MaterialTheme.typography.bodySmallEmphasized,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceColorAtElevation(
                                            8.dp
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.add_24px),
                                    contentDescription = "Add schedule",
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
}

private val PreviewSessionWithSubjectAndInstructor = DayOfWeek.entries.flatMap { dayOfWeek ->
    List(5) {
        SessionAndSubjectAndInstructor(
            session = if (it == 0 || it == 1) {
                Session.subjectSession(
                    timeTableId = 1,
                    dayOfWeek = dayOfWeek,
                    startTime = LocalTime.of(it, 0),
                    subjectId = 0
                )
            } else {
                Session.emptySession(1, dayOfWeek, LocalTime.of(it, 0))
            }, subjectWithInstructor = if (it == 0 || it == 1) {
                SubjectWithInstructor(
                    Subject(
                        code = "Math 123",
                        description = "Mathematics literature",
                        color = Color.Blue.toArgb(),
                        instructorId = 0
                    ), Instructor(name = "John Doe")
                )
            } else {
                null
            }
        )
    }
}

@Preview(showBackground = true, widthDp = 500)
@Composable
private fun EditModeGridPreview() {
    Row {
        EditModeGrid(sessionsWithSubjectAndInstructor = PreviewSessionWithSubjectAndInstructor,
            onGridClick = {})
    }
}

@Preview(showBackground = true, widthDp = 500)
@Composable
private fun DefaultModeGridPreview() {
    Row {
        DefaultModeGrid(dayScheduleMap = PreviewSessionWithSubjectAndInstructor.toMappedSchedules(),
            onChangeToEditMode = { })
    }
}


private val RowHeight = 72.dp

enum class CellBorderDirection {
    Horizontal, Vertical
}