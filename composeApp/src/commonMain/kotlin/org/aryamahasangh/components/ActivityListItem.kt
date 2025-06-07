package org.aryamahasangh.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import aryamahasangh.composeapp.generated.resources.Res
import aryamahasangh.composeapp.generated.resources.event_upcoming
import dev.burnoo.compose.remembersetting.rememberBooleanSetting
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.aryamahasangh.SettingKeys
import org.aryamahasangh.features.activities.ActivityType
import org.aryamahasangh.features.activities.OrganisationalActivityShort
import org.aryamahasangh.features.arya_nirman.convertDates
import org.aryamahasangh.utils.WithTooltip
import org.aryamahasangh.utils.formatShort
import org.jetbrains.compose.resources.painterResource

val activityTypeData = ActivityType.entries.associateWith { it.toDisplayName() }


@Composable
fun ActivityListItem(
  activity: OrganisationalActivityShort,
  handleOnClick: () -> Unit = {},
  handleDeleteActivity: () -> Unit = {},
  handleEditActivity: () -> Unit = {}
) {
  val startDate = formatShort(activity.startDatetime)
  val endDate = formatShort(activity.endDatetime)
  var isLoggedIn by rememberBooleanSetting(SettingKeys.isLoggedIn, false)
  var showConfirmDialog by remember { mutableStateOf(false) }
  var showMenu by remember { mutableStateOf(false) }

  // Determine activity status
  val activityStatus = getActivityStatus(activity.startDatetime, activity.endDatetime)

  ElevatedCard(
    modifier = Modifier.width(500.dp),
    shape = RoundedCornerShape(4.dp),
    onClick = handleOnClick
  ) {
    Box(
      modifier = Modifier.padding(8.dp).fillMaxWidth()
    ) {
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Top
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = activity.name,
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                modifier =
                  Modifier.background(
                    MaterialTheme.colorScheme.outlineVariant
                  ).padding(vertical = 1.dp, horizontal = 8.dp),
                text = "${activityTypeData[activity.type]}",
                style = MaterialTheme.typography.bodyLarge
              )
            }
          }
          if (isLoggedIn) {
            Box {
              IconButton(
                onClick = { showMenu = true }
              ) {
                Icon(
                  Icons.Default.MoreVert,
                  contentDescription = "More options",
                  Modifier.size(24.dp)
                )
              }
              DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
              ) {
                if(activityStatus != ActivityStatus.PAST) {
                  DropdownMenuItem(
                    text = { Text("सम्पादित करें") },
                    onClick = {
                      showMenu = false
                      handleEditActivity()
                    },
                    leadingIcon = {
                      Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        Modifier.size(24.dp)
                      )
                    }
                  )
                }
                DropdownMenuItem(
                  text = { Text("गतिविधि हटाएँ") },
                  onClick = {
                    showMenu = false
                    showConfirmDialog = true
                  },
                  leadingIcon = {
                    Icon(
                      Icons.Default.Delete,
                      contentDescription = "Edit",
                      Modifier.size(24.dp)
                    )
                  }
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = activity.shortDescription,
          style = MaterialTheme.typography.bodyMedium,
          maxLines = 1
        )
        Spacer(modifier = Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
          val subtleTextColor = MaterialTheme.colorScheme.onSurfaceVariant // Get from theme
          Icon(imageVector = Icons.Default.DateRange, contentDescription = "Date", tint = Color.Gray)
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text =
              buildAnnotatedString {
                val (dateRange, timeRange) = convertDates(activity.startDatetime, activity.endDatetime)
                withStyle(
                  style =
                    SpanStyle(
                      fontWeight = FontWeight.SemiBold,
                      fontSize = 16.sp,
                      color = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                  append(dateRange)
                }
                append(" | ")
                withStyle(style = SpanStyle(fontSize = 13.sp, color = subtleTextColor)) {
                  append(timeRange)
                }
              },
            style = MaterialTheme.typography.bodyMedium
          )
//          Text(text = "प्रारंभ: $startDate", style = MaterialTheme.typography.bodySmall)
//          Spacer(modifier = Modifier.width(4.dp))
//          Text(text = "समाप्ति: $endDate", style = MaterialTheme.typography.bodySmall)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth().padding(end = 8.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.LocationOn, contentDescription = "Place", tint = Color.Gray)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "${activity.district}, ${activity.state}",
              style = MaterialTheme.typography.bodySmall,
            )
          }
          WithTooltip(activityStatus.toDisplayName()){
            // Activity status indicator
            StatusChip(status = activityStatus)
          }
        }
      }
    }
    if (showConfirmDialog) {
      AlertDialog(
        onDismissRequest = { showConfirmDialog = false },
        title = { Text("गतिविधि हटाएँ") },
        text = { Text("क्या आप वाकई इस गतिविधि को हटाना चाहते हैं?") },
        confirmButton = {
          TextButton(onClick = {
            showConfirmDialog = false
            handleDeleteActivity()
          }) {
            Text("हाँ")
          }
        },
        dismissButton = {
          TextButton(onClick = { showConfirmDialog = false }) {
            Text("नहीं")
          }
        }
      )
    }
  }
}

fun getActivityStatus(
  startDatetime: LocalDateTime,
  endDatetime: LocalDateTime,
): ActivityStatus {
  val currentTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
  return when {
    currentTime < startDatetime -> ActivityStatus.UPCOMING
    currentTime > endDatetime -> ActivityStatus.PAST
    else -> ActivityStatus.ONGOING
  }
}

enum class ActivityStatus {
  PAST,
  ONGOING,
  UPCOMING;

  fun toDisplayName(): String {
    return when(this){
      ActivityStatus.PAST -> "समाप्त"
      ActivityStatus.ONGOING -> "चल रही है"
      ActivityStatus.UPCOMING -> "आगामी"
    }
  }
}

@Composable
fun StatusChip(status: ActivityStatus) {
  EventIcon(status)
}

@Composable
fun EventIcon(
  state: ActivityStatus,
  modifier: Modifier = Modifier,
  size: Dp = 24.dp
) {
  when (state) {
    ActivityStatus.PAST -> PastIcon(modifier.size(size))
    ActivityStatus.ONGOING -> OngoingIcon(modifier.size(size))
    ActivityStatus.UPCOMING -> FutureIcon(modifier.size(size))
  }
}

@Composable
fun PastIcon(modifier: Modifier = Modifier) {
  Icon(
    imageVector = Icons.Default.History,
    contentDescription = "Past Event",
    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
    modifier = modifier
  )
}


@Composable
fun OngoingIcon(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
  val rippleRadius = remember { Animatable(0f) }

  LaunchedEffect(Unit) {
    while (true) {
      rippleRadius.snapTo(0f)
      rippleRadius.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1500, easing = LinearEasing)
      )
    }
  }

  Canvas(modifier = modifier.size(48.dp)) {
    val center = Offset(size.width / 2, size.height / 2)
    val maxRadius = size.minDimension / 2

    // Draw 3 ripples at different phases
    for (i in 0..2) {
      val progress = (rippleRadius.value + i * 0.33f) % 1f
      val alpha = 1f - progress
      val radius = progress * maxRadius
      drawCircle(
        color = color.copy(alpha = alpha),
        radius = radius,
        center = center,
        style = Stroke(width = 2.dp.toPx())
      )
    }

    // Draw a solid center
    drawCircle(
      color = color,
      radius = maxRadius * 0.2f,
      center = center
    )
  }
}

@Composable
fun FutureIcon(modifier: Modifier = Modifier) {
  val bounceAnim = remember { Animatable(0.7f) }

  LaunchedEffect(Unit) {
    bounceAnim.animateTo(
      targetValue = 1f,
      animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
      )
    )
  }

  Icon(
    painter = painterResource(Res.drawable.event_upcoming),
    contentDescription = "Future Event",
    modifier = modifier.scale(bounceAnim.value),
    tint = MaterialTheme.colorScheme.secondary
  )
}
