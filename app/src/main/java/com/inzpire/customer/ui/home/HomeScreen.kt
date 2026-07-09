package com.inzpire.customer.ui.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inzpire.customer.data.Cockpit
import com.inzpire.customer.data.CockpitData
import com.inzpire.customer.data.CockpitData.MilestoneStatus
import com.inzpire.customer.data.formatINRCompact
import com.inzpire.customer.ui.components.BrandGradientBox
import com.inzpire.customer.ui.components.ProgressBar
import com.inzpire.customer.ui.components.RemoteImage
import com.inzpire.customer.ui.components.SectionHeader
import com.inzpire.customer.ui.components.SurfaceCard
import com.inzpire.customer.ui.theme.Background
import com.inzpire.customer.ui.theme.Border
import com.inzpire.customer.ui.theme.Destructive
import com.inzpire.customer.ui.theme.Foreground
import com.inzpire.customer.ui.theme.Gold
import com.inzpire.customer.ui.theme.MutedForeground
import com.inzpire.customer.ui.theme.Navy
import com.inzpire.customer.ui.theme.Sky
import com.inzpire.customer.ui.theme.SkySoft
import com.inzpire.customer.ui.theme.Success
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dMMM: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
private val dMMMyyyy: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

@Composable
fun HomeScreen(
    cockpit: Cockpit,
    customerName: String?,
    onOpenApprovals: () -> Unit,
    onOpenDesigns: () -> Unit,
    onOpenMaterials: () -> Unit,
    onOpenPayments: () -> Unit,
    onOpenDocuments: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenOffers: () -> Unit,
    onOpenEnquiry: () -> Unit,
) {
    val project = cockpit.project
    val dl = CockpitData.daysLineFor(project, LocalDate.now())
    val paid = cockpit.payments.filter { it.status == CockpitData.PaymentStatus.PAID }.sumOf { it.amount }
    val pendingApprovals = cockpit.approvals.count { it.status == CockpitData.ReviewStatus.PENDING }
    val firstName = (customerName ?: CockpitData.customer.name).substringBefore(' ')

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // Greeting
        Column {
            Text("Hi $firstName", color = MutedForeground, fontSize = 13.sp)
            Text("Your project, all in one place", color = Navy, fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp)
        }

        // Project header card
        SurfaceCard {
            Column {
                BrandGradientBox(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(project.code, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Text("${project.title} — ${customerName ?: CockpitData.customer.name}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 2.dp))
                                Text(
                                    listOfNotNull(project.segment, project.location.ifBlank { null }).joinToString(" · "),
                                    color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Gold)
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                            ) {
                                Text("Stage ${project.currentStage} / 6", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Days line
                        Row(
                            Modifier.fillMaxWidth().padding(top = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Column {
                                Text("Day ${dl.elapsed}", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 32.sp)
                                Text("of ${dl.total} days", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    if (dl.overdue) "Overdue" else "${dl.remaining} days left",
                                    color = if (dl.overdue) Color(0xFFFF9AA0) else Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    "${project.startDate.format(dMMM)} → ${project.targetHandoverDate.format(dMMM)}",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 11.sp,
                                )
                            }
                        }
                        ProgressBar(
                            fraction = dl.pct,
                            modifier = Modifier.padding(top = 8.dp),
                            fillColor = if (dl.overdue) Destructive else Gold,
                        )
                    }
                }

                // Team
                if (cockpit.team.isNotEmpty()) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        cockpit.team.take(2).forEach { person ->
                            TeamCard(person, onOpenChat, Modifier.weight(1f))
                        }
                        if (cockpit.team.size == 1) androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        // Milestone tracker
        Column {
            SectionHeader("Project journey", trailing = { Text("${project.percentComplete}% complete", color = MutedForeground, fontSize = 12.sp) })
            SurfaceCard {
                Column {
                    cockpit.milestones.forEachIndexed { i, m ->
                        if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(Border))
                        MilestoneRow(index = i, milestone = m)
                    }
                }
            }
        }

        // Quick access
        Column {
            SectionHeader("Quick access")
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickTile(Icons.Filled.AssignmentTurnedIn, "Approvals", "$pendingApprovals pending", pendingApprovals > 0, onOpenApprovals, Modifier.weight(1f))
                    QuickTile(Icons.Filled.Image, "Designs", "Boards, 2D & 3D", false, onOpenDesigns, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickTile(Icons.Filled.Palette, "Materials", "Finishes & swatches", false, onOpenMaterials, Modifier.weight(1f))
                    QuickTile(Icons.Filled.AccountBalanceWallet, "Payments", "${formatINRCompact(paid)} of ${formatINRCompact(project.value)}", false, onOpenPayments, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickTile(Icons.Filled.Description, "Documents", "Contract, BOQ, invoices", false, onOpenDocuments, Modifier.weight(1f))
                    QuickTile(Icons.Outlined.ChatBubbleOutline, "Chat", "Message your team", false, onOpenChat, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickTile(Icons.Filled.LocalOffer, "Offers", "Live promos", false, onOpenOffers, Modifier.weight(1f))
                    QuickTile(Icons.Filled.PostAdd, "New enquiry", "Start a project", false, onOpenEnquiry, Modifier.weight(1f))
                }
            }
        }

        // Site updates
        if (cockpit.siteUpdates.isNotEmpty()) {
            Column {
                SectionHeader("Site updates")
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    cockpit.siteUpdates.forEach { u ->
                        SurfaceCard(Modifier.fillMaxWidth()) {
                            Column {
                                if (u.imageUrl.isNotBlank()) {
                                    RemoteImage(u.imageUrl, u.note, Modifier.fillMaxWidth().height(160.dp), ContentScale.Crop)
                                }
                                Column(Modifier.padding(12.dp)) {
                                    Text(u.note, color = Foreground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        listOfNotNull(u.geotag.ifBlank { null }, u.createdAt.format(dMMMyyyy)).joinToString(" · "),
                                        color = MutedForeground, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamCard(person: CockpitData.Person, onChat: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RemoteImage(person.photoUrl, person.name, Modifier.size(40.dp).clip(CircleShape), ContentScale.Crop)
        Column(Modifier.weight(1f)) {
            Text(person.name, color = Foreground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(person.role, color = MutedForeground, fontSize = 11.sp, maxLines = 1)
        }
        if (person.phone.isNotBlank()) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(SkySoft)
                    .clickable { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${person.phone}"))) }
                    .padding(7.dp),
            ) {
                Icon(Icons.Filled.Call, contentDescription = "Call ${person.name}", tint = Navy, modifier = Modifier.size(15.dp))
            }
        }
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(SkySoft)
                .clickable(onClick = onChat)
                .padding(7.dp),
        ) {
            Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Chat", tint = Navy, modifier = Modifier.size(15.dp))
        }
    }
}

@Composable
private fun MilestoneRow(index: Int, milestone: CockpitData.Milestone) {
    val (icon, tint) = when (milestone.status) {
        MilestoneStatus.DONE -> Icons.Filled.CheckCircle to Success
        MilestoneStatus.IN_PROGRESS -> Icons.Filled.Autorenew to Sky
        MilestoneStatus.UPCOMING -> Icons.Filled.RadioButtonUnchecked to MutedForeground
    }
    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("STEP ${index + 1}", color = MutedForeground, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                if (milestone.status == MilestoneStatus.IN_PROGRESS) {
                    Box(Modifier.clip(RoundedCornerShape(50)).background(Sky.copy(alpha = 0.15f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("In progress", color = Sky, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Text(milestone.name, color = Foreground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            milestone.date?.let { Text(it.format(dMMMyyyy), color = MutedForeground, fontSize = 11.sp) }
        }
        milestone.thumbnailUrl?.let {
            RemoteImage(it, null, Modifier.size(width = 60.dp, height = 44.dp).clip(RoundedCornerShape(8.dp)), ContentScale.Crop)
        }
    }
}

@Composable
private fun QuickTile(
    icon: ImageVector,
    title: String,
    desc: String,
    highlight: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SurfaceCard(modifier = modifier, onClick = onClick) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (highlight) Navy else SkySoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = if (highlight) Color.White else Navy, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, color = Foreground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(desc, color = MutedForeground, fontSize = 11.sp, maxLines = 1)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MutedForeground, modifier = Modifier.size(18.dp))
        }
    }
}
