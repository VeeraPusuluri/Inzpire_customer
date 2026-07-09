package com.inzpire.customer.data

import com.inzpire.customer.data.CockpitData.ApprovalKind
import com.inzpire.customer.data.CockpitData.DesignType
import com.inzpire.customer.data.CockpitData.MaterialStatus
import com.inzpire.customer.data.CockpitData.MilestoneStatus
import com.inzpire.customer.data.CockpitData.PaymentStatus
import com.inzpire.customer.data.CockpitData.ReviewStatus
import com.inzpire.customer.data.model.ApprovalDto
import com.inzpire.customer.data.model.ApprovalPatch
import com.inzpire.customer.data.model.DocumentDto
import com.inzpire.customer.data.model.MoodboardDto
import com.inzpire.customer.data.model.MoodboardPatch
import com.inzpire.customer.data.model.PaymentDto
import com.inzpire.customer.data.model.PaymentPatch
import com.inzpire.customer.data.model.ProductDto
import com.inzpire.customer.data.model.ProfileLiteDto
import com.inzpire.customer.data.model.ProjectDto
import com.inzpire.customer.data.model.RoomDto
import com.inzpire.customer.data.model.SelectionDto
import com.inzpire.customer.data.model.SiteUpdateDto
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate
import java.time.OffsetDateTime

/**
 * A fully-mapped snapshot of the signed-in customer's project cockpit, in the
 * exact shape the screens already render. Loaded live from Supabase; when there
 * is no project yet (or the fetch fails) the screens fall back to [CockpitData].
 */
data class Cockpit(
    val projectId: String,
    val project: CockpitData.Project,
    val team: List<CockpitData.Person>,
    val milestones: List<CockpitData.Milestone>,
    val designs: List<CockpitData.Design>,
    val materials: List<CockpitData.Material>,
    val payments: List<CockpitData.Payment>,
    val approvals: List<CockpitData.Approval>,
    val siteUpdates: List<CockpitData.SiteUpdate>,
    val documents: List<CockpitData.Document>,
) {
    companion object {
        /** The bundled seed — used before the first load and as an offline fallback. */
        val seed: Cockpit = Cockpit(
            projectId = CockpitData.project.id,
            project = CockpitData.project,
            team = CockpitData.team,
            milestones = CockpitData.milestones,
            designs = CockpitData.designs,
            materials = CockpitData.materials,
            payments = CockpitData.payments,
            approvals = CockpitData.approvals,
            siteUpdates = CockpitData.siteUpdates,
            documents = CockpitData.documents,
        )
    }
}

class CockpitRepository(
    private val client: io.github.jan.supabase.SupabaseClient = SupabaseClientProvider.client,
) {

    /** Returns the customer's most recent project fully mapped, or null if they have none. */
    suspend fun load(userId: String, customerLocation: String?): Cockpit? {
        val projectDto = client.postgrest["projects"].select {
            filter { eq("customer_id", userId) }
            order("created_at", Order.DESCENDING)
            limit(1L)
        }.decodeList<ProjectDto>().firstOrNull() ?: return null

        val pid = projectDto.id

        return coroutineScope {
            val teamJob = async { loadTeam(projectDto) }
            val designsJob = async {
                client.postgrest["moodboards"].select {
                    filter { eq("project_id", pid) }
                    order("created_at", Order.ASCENDING)
                }.decodeList<MoodboardDto>()
            }
            val materialsJob = async { loadMaterials(pid) }
            val paymentsJob = async {
                client.postgrest["payments"].select {
                    filter { eq("project_id", pid) }
                    order("created_at", Order.ASCENDING)
                }.decodeList<PaymentDto>()
            }
            val approvalsJob = async {
                client.postgrest["approvals"].select {
                    filter { eq("project_id", pid) }
                    order("created_at", Order.ASCENDING)
                }.decodeList<ApprovalDto>()
            }
            val updatesJob = async {
                client.postgrest["site_updates"].select {
                    filter { eq("project_id", pid) }
                    order("created_at", Order.DESCENDING)
                }.decodeList<SiteUpdateDto>()
            }
            val docsJob = async {
                runCatching {
                    client.postgrest["project_documents"].select {
                        filter { eq("project_id", pid) }
                        order("created_at", Order.DESCENDING)
                    }.decodeList<DocumentDto>()
                }.getOrDefault(emptyList())
            }

            Cockpit(
                projectId = pid,
                project = mapProject(projectDto, customerLocation),
                team = teamJob.await(),
                milestones = milestonesForStage(projectDto.stage),
                designs = designsJob.await().map(::mapDesign),
                materials = materialsJob.await(),
                payments = paymentsJob.await().map(::mapPayment),
                approvals = approvalsJob.await().map(::mapApproval),
                siteUpdates = updatesJob.await().mapNotNull(::mapSiteUpdate),
                documents = docsJob.await().map(::mapDocument),
            )
        }
    }

    private suspend fun loadTeam(project: ProjectDto): List<CockpitData.Person> {
        val ids = listOfNotNull(project.designerId, project.managerId)
        if (ids.isEmpty()) return emptyList()
        val profiles = client.postgrest["profiles"].select {
            filter { isIn("id", ids) }
        }.decodeList<ProfileLiteDto>()
        // Preserve designer-then-PM order to match the seed layout.
        return ids.mapNotNull { id -> profiles.firstOrNull { it.id == id } }.map { p ->
            val role = when (p.id) {
                project.designerId -> "Interior Designer"
                project.managerId -> "Project Manager"
                else -> "Team"
            }
            CockpitData.Person(
                id = p.id,
                name = p.name ?: role,
                role = role,
                phone = p.phone ?: "",
                photoUrl = p.photoUrl ?: "",
            )
        }
    }

    private suspend fun loadMaterials(projectId: String): List<CockpitData.Material> {
        val rooms = client.postgrest["rooms"].select {
            filter { eq("project_id", projectId) }
            order("created_at", Order.ASCENDING)
        }.decodeList<RoomDto>()
        if (rooms.isEmpty()) return emptyList()
        val roomName = rooms.associate { it.id to it.name }

        val selections = client.postgrest["selections"].select(
            columns = Columns.list("id", "category", "make", "status", "room_id", "product_id"),
        ) {
            filter { isIn("room_id", rooms.map { it.id }) }
        }.decodeList<SelectionDto>()
        if (selections.isEmpty()) return emptyList()

        val productIds = selections.mapNotNull { it.productId }.distinct()
        val products = if (productIds.isEmpty()) emptyList() else
            client.postgrest["products"].select {
                filter { isIn("id", productIds) }
            }.decodeList<ProductDto>()
        val productById = products.associateBy { it.id }

        return selections.map { s ->
            val prod = s.productId?.let { productById[it] }
            CockpitData.Material(
                id = s.id,
                room = roomName[s.roomId] ?: "Room",
                category = s.category ?: prod?.category ?: "Material",
                name = prod?.description ?: s.category ?: "Selection",
                make = s.make ?: prod?.makeNotes ?: "",
                swatchUrl = prod?.imageUrl ?: "",
                status = when (s.status) {
                    "locked" -> MaterialStatus.LOCKED
                    "selected" -> MaterialStatus.SELECTED
                    else -> MaterialStatus.SUGGESTED
                },
            )
        }
    }

    // ---- write actions ----

    suspend fun approveDesign(moodboardId: String) =
        client.postgrest["moodboards"].update(MoodboardPatch(status = "approved")) {
            filter { eq("id", moodboardId) }
        }

    suspend fun requestDesignChanges(moodboardId: String, comment: String?) =
        client.postgrest["moodboards"].update(MoodboardPatch(status = "revision", comment = comment)) {
            filter { eq("id", moodboardId) }
        }

    suspend fun approveApproval(id: String, approverId: String) =
        client.postgrest["approvals"].update(
            ApprovalPatch(status = "approved", approverId = approverId, signedAt = OffsetDateTime.now().toString()),
        ) { filter { eq("id", id) } }

    suspend fun rejectApproval(id: String, comment: String?) =
        client.postgrest["approvals"].update(ApprovalPatch(status = "rejected", comment = comment)) {
            filter { eq("id", id) }
        }

    suspend fun markPaymentPaid(id: String) =
        client.postgrest["payments"].update(PaymentPatch(status = "paid", paidAt = OffsetDateTime.now().toString())) {
            filter { eq("id", id) }
        }

    // ---- mapping helpers ----

    private fun mapProject(p: ProjectDto, customerLocation: String?): CockpitData.Project {
        val start = parseDbDate(p.startDate) ?: LocalDate.now()
        val handover = parseDbDate(p.targetHandoverDate) ?: start.plusDays(p.targetDays.toLong())
        return CockpitData.Project(
            id = p.id,
            title = p.name ?: "Project",
            code = p.code ?: "INZ",
            segment = p.segment?.replaceFirstChar { it.uppercase() } ?: "Residential",
            location = customerLocation ?: "",
            startDate = start,
            targetDays = p.targetDays,
            targetHandoverDate = handover,
            value = p.value,
            currentStage = p.stage.coerceIn(1, 6),
            percentComplete = (p.stage.coerceIn(1, 6) * 100) / 6,
        )
    }

    private fun mapDesign(m: MoodboardDto) = CockpitData.Design(
        id = m.id,
        room = m.room ?: "Room",
        title = m.title,
        type = when (m.type) {
            "three_d" -> DesignType.THREE_D
            "two_d" -> DesignType.TWO_D
            else -> DesignType.MOODBOARD
        },
        imageUrl = m.mediaUrls.firstOrNull() ?: "",
        version = "v${m.version}",
        status = when (m.status) {
            "approved" -> ReviewStatus.APPROVED
            "revision" -> ReviewStatus.CHANGES
            else -> ReviewStatus.PENDING
        },
    )

    private fun mapPayment(p: PaymentDto) = CockpitData.Payment(
        id = p.id,
        milestone = p.milestone,
        amount = p.amount,
        status = if (p.status == "paid") PaymentStatus.PAID else PaymentStatus.PENDING,
        paidAt = parseDbDate(p.paidAt),
    )

    private fun mapApproval(a: ApprovalDto) = CockpitData.Approval(
        id = a.id,
        kind = when (a.type) {
            "design" -> ApprovalKind.DESIGN
            "material" -> ApprovalKind.MATERIAL
            else -> ApprovalKind.QUOTE
        },
        title = a.title ?: a.type.replaceFirstChar { it.uppercase() },
        thumbnailUrl = null,
        status = when (a.status) {
            "approved" -> ReviewStatus.APPROVED
            "rejected" -> ReviewStatus.CHANGES
            else -> ReviewStatus.PENDING
        },
        approver = if (a.status == "approved") "You" else null,
        signedAt = parseDbDate(a.signedAt),
        sentAt = parseDbDate(a.createdAt) ?: LocalDate.now(),
    )

    private fun mapSiteUpdate(u: SiteUpdateDto): CockpitData.SiteUpdate? {
        val date = parseDbDate(u.createdAt) ?: return null
        return CockpitData.SiteUpdate(
            id = u.id,
            note = u.note ?: "Site update",
            geotag = u.geotag ?: "",
            createdAt = date,
            imageUrl = u.mediaUrls.firstOrNull() ?: "",
        )
    }

    private fun mapDocument(d: DocumentDto) = CockpitData.Document(
        id = d.id,
        name = d.name,
        type = d.docType ?: "File",
        date = parseDbDate(d.createdAt) ?: LocalDate.now(),
    )

    private fun milestonesForStage(stage: Int): List<CockpitData.Milestone> {
        val names = listOf(
            "Drawings Stage", "Material Selection Stage", "Final Approval",
            "Production", "Site Execution", "Handover",
        )
        val s = stage.coerceIn(1, 6)
        return names.mapIndexed { i, name ->
            val step = i + 1
            val status = when {
                step < s -> MilestoneStatus.DONE
                step == s -> MilestoneStatus.IN_PROGRESS
                else -> MilestoneStatus.UPCOMING
            }
            CockpitData.Milestone(name = name, status = status)
        }
    }

    private fun parseDbDate(value: String?): LocalDate? {
        if (value.isNullOrBlank()) return null
        return runCatching { LocalDate.parse(value) }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(value).toLocalDate() }.getOrNull()
    }
}
