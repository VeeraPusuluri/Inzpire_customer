package com.inzpire.customer.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 1:1 port of the web app's `src/lib/inzpire/cockpit-data.ts`.
 * The customer cockpit (Home / Designs / Materials / Pay / Approvals) "renders entirely off this
 * data" — the customer is an already-converted account (NM Reddy — 3BHK Flat). Image fields hold
 * the same Unsplash URLs, loaded via Coil.
 */
object CockpitData {

    val TODAY: LocalDate = LocalDate.parse("2026-06-15")

    data class Person(
        val id: String,
        val name: String,
        val role: String,
        val phone: String,
        val photoUrl: String,
    )

    data class Project(
        val id: String,
        val title: String,
        val code: String,
        val segment: String,
        val location: String,
        val startDate: LocalDate,
        val targetDays: Int,
        val targetHandoverDate: LocalDate,
        val value: Double,
        val currentStage: Int,
        val percentComplete: Int,
    )

    enum class MilestoneStatus { DONE, IN_PROGRESS, UPCOMING }
    enum class ReviewStatus(val label: String) { APPROVED("Approved"), PENDING("Pending"), CHANGES("Changes") }
    enum class DesignType(val label: String) { MOODBOARD("Moodboard"), TWO_D("2D"), THREE_D("3D") }
    enum class MaterialStatus(val label: String) { SUGGESTED("Suggested"), SELECTED("Selected"), LOCKED("Locked") }
    enum class ApprovalKind(val label: String) { DESIGN("Design"), MATERIAL("Material"), QUOTE("Quote") }
    enum class PaymentStatus { PAID, PENDING }

    data class Milestone(
        val name: String,
        val status: MilestoneStatus,
        val date: LocalDate? = null,
        val thumbnailUrl: String? = null,
    )

    data class Design(
        val id: String,
        val room: String,
        val title: String,
        val type: DesignType,
        val imageUrl: String,
        val version: String,
        val status: ReviewStatus,
    )

    data class Material(
        val id: String,
        val room: String,
        val category: String,
        val name: String,
        val make: String,
        val swatchUrl: String,
        val status: MaterialStatus,
        val mediaUrls: List<String> = emptyList(),
    )

    data class Approval(
        val id: String,
        val kind: ApprovalKind,
        val title: String,
        val thumbnailUrl: String?,
        val status: ReviewStatus,
        val approver: String? = null,
        val signedAt: LocalDate? = null,
        val sentAt: LocalDate,
    )

    data class Payment(
        val id: String,
        val milestone: String,
        val amount: Double,
        val status: PaymentStatus,
        val paidAt: LocalDate? = null,
    )

    data class SiteUpdate(
        val id: String,
        val note: String,
        val geotag: String,
        val createdAt: LocalDate,
        val imageUrl: String,
    )

    data class Document(val id: String, val name: String, val type: String, val date: LocalDate, val url: String = "")

    val customer = Person(
        id = "cust_001",
        name = "NM Reddy",
        role = "Customer",
        phone = "+9199xxxxxx01",
        photoUrl = "https://images.unsplash.com/photo-1633332755192-727a05c4013d?w=240&q=80&auto=format&fit=crop",
    )

    val designer = Person(
        id = "stf_des",
        name = "Sneha Rao",
        role = "Interior Designer",
        phone = "+9199xxxxxx10",
        photoUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=200&q=80&auto=format&fit=crop",
    )

    val pm = Person(
        id = "stf_pm",
        name = "Karthik Verma",
        role = "Project Manager",
        phone = "+9199xxxxxx11",
        photoUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&q=80&auto=format&fit=crop",
    )

    val team = listOf(designer, pm)

    val project = Project(
        id = "proj_001",
        title = "3BHK Flat",
        code = "INZ-2026-014",
        segment = "Residential",
        location = "Manikonda, Hyderabad",
        startDate = LocalDate.parse("2026-05-20"),
        targetDays = 50,
        targetHandoverDate = LocalDate.parse("2026-07-09"),
        value = 1_850_000.0,
        currentStage = 2,
        percentComplete = 35,
    )

    val milestones = listOf(
        Milestone("Drawings Stage", MilestoneStatus.DONE, LocalDate.parse("2026-05-28"),
            "https://images.unsplash.com/photo-1503387762-592deb58ef4e?w=400&q=80&auto=format&fit=crop"),
        Milestone("Material Selection Stage", MilestoneStatus.IN_PROGRESS, LocalDate.parse("2026-06-12"),
            "https://images.unsplash.com/photo-1581291518857-4e27b48ff24e?w=400&q=80&auto=format&fit=crop"),
        Milestone("Final Approval", MilestoneStatus.UPCOMING),
        Milestone("Production", MilestoneStatus.UPCOMING),
        Milestone("Site Execution", MilestoneStatus.UPCOMING),
        Milestone("Handover", MilestoneStatus.UPCOMING, LocalDate.parse("2026-07-09")),
    )

    val designs = listOf(
        Design("d1", "Living Room", "Living — Modern Warm", DesignType.THREE_D,
            "https://images.unsplash.com/photo-1616486338812-3dadae4b4ace?w=900&q=80&auto=format&fit=crop", "v2", ReviewStatus.APPROVED),
        Design("d2", "Kitchen", "Modular Kitchen Layout", DesignType.TWO_D,
            "https://images.unsplash.com/photo-1556909114-f6e7ad7d3136?w=900&q=80&auto=format&fit=crop", "v1", ReviewStatus.APPROVED),
        Design("d3", "Master Bedroom", "Master — Calm Luxe", DesignType.THREE_D,
            "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=900&q=80&auto=format&fit=crop", "v1", ReviewStatus.PENDING),
        Design("d4", "Kids Bedroom", "Kids Room Concept", DesignType.MOODBOARD,
            "https://images.unsplash.com/photo-1558877385-8c1f51fdbf2c?w=900&q=80&auto=format&fit=crop", "v1", ReviewStatus.PENDING),
    )

    val materials = listOf(
        Material("m1", "Living Room", "Laminate", "Walnut Natural", "Merino",
            "https://images.unsplash.com/photo-1604147706283-d7119b5b822c?w=400&q=80&auto=format&fit=crop", MaterialStatus.LOCKED),
        Material("m2", "Living Room", "Flooring", "Italian Marble", "Kajaria",
            "https://images.unsplash.com/photo-1615873968403-89e068629265?w=400&q=80&auto=format&fit=crop", MaterialStatus.SELECTED),
        Material("m3", "Kitchen", "Countertop", "Quartz Carrara", "Caesarstone",
            "https://images.unsplash.com/photo-1556910103-1c02745aae4d?w=400&q=80&auto=format&fit=crop", MaterialStatus.SELECTED),
        Material("m4", "Kitchen", "Hardware", "Soft-close Hinges", "Hettich",
            "https://images.unsplash.com/photo-1581858726788-75bc0f6a952d?w=400&q=80&auto=format&fit=crop", MaterialStatus.LOCKED),
        Material("m5", "Master Bedroom", "Veneer", "Teak Veneer", "Greenply",
            "https://images.unsplash.com/photo-1610701596007-11502861dcfa?w=400&q=80&auto=format&fit=crop", MaterialStatus.SUGGESTED),
        Material("m6", "Master Bedroom", "Paint", "Warm Ivory", "Asian Royale",
            "https://images.unsplash.com/photo-1562184552-997c461cc6f6?w=400&q=80&auto=format&fit=crop", MaterialStatus.SUGGESTED),
    )

    val approvals = listOf(
        Approval("a1", ApprovalKind.DESIGN, "Living Room 3D — v2", designs[0].imageUrl,
            ReviewStatus.APPROVED, "NM Reddy", LocalDate.parse("2026-06-02"), LocalDate.parse("2026-05-30")),
        Approval("a2", ApprovalKind.DESIGN, "Master Bedroom 3D — v1", designs[2].imageUrl,
            ReviewStatus.PENDING, sentAt = LocalDate.parse("2026-06-10")),
        Approval("a3", ApprovalKind.MATERIAL, "Living Room Material Board", materials[1].swatchUrl,
            ReviewStatus.PENDING, sentAt = LocalDate.parse("2026-06-12")),
        Approval("a4", ApprovalKind.QUOTE, "Revised BOQ — v2", null,
            ReviewStatus.APPROVED, "NM Reddy", LocalDate.parse("2026-06-05"), LocalDate.parse("2026-06-04")),
    )

    val payments = listOf(
        Payment("p1", "Booking (30%)", 555_000.0, PaymentStatus.PAID, LocalDate.parse("2026-05-20")),
        Payment("p2", "Production (30%)", 555_000.0, PaymentStatus.PENDING),
        Payment("p3", "Installation (30%)", 555_000.0, PaymentStatus.PENDING),
        Payment("p4", "Handover (10%)", 185_000.0, PaymentStatus.PENDING),
    )

    val siteUpdates = listOf(
        SiteUpdate("s1", "Civil & false ceiling framework started", "Manikonda", LocalDate.parse("2026-06-13"),
            "https://images.unsplash.com/photo-1503387837-b154d5074bd2?w=800&q=80&auto=format&fit=crop"),
        SiteUpdate("s2", "Kitchen base units measured on site", "Manikonda", LocalDate.parse("2026-06-10"),
            "https://images.unsplash.com/photo-1556909114-44e3e9399a2f?w=800&q=80&auto=format&fit=crop"),
    )

    val documents = listOf(
        Document("doc1", "Signed Contract.pdf", "Contract", LocalDate.parse("2026-05-18")),
        Document("doc2", "Invoice — Booking.pdf", "Invoice", LocalDate.parse("2026-05-20")),
        Document("doc3", "Final BOQ v2.pdf", "BOQ", LocalDate.parse("2026-06-05")),
        Document("doc4", "Working Drawings.pdf", "Drawing", LocalDate.parse("2026-05-28")),
    )

    data class DaysLine(
        val elapsed: Long,
        val total: Int,
        val remaining: Long,
        val pct: Float,
        val overdue: Boolean,
    )

    fun daysLine(today: LocalDate = TODAY): DaysLine = daysLineFor(project, today)

    /** Day-line for any project (live or seed). */
    fun daysLineFor(p: Project, today: LocalDate): DaysLine {
        val elapsed = maxOf(0, ChronoUnit.DAYS.between(p.startDate, today))
        val remaining = ChronoUnit.DAYS.between(today, p.targetHandoverDate)
        val pct = (elapsed.toFloat() / p.targetDays.coerceAtLeast(1)).coerceIn(0f, 1f)
        return DaysLine(
            elapsed = elapsed,
            total = p.targetDays,
            remaining = remaining,
            pct = pct,
            overdue = remaining < 0,
        )
    }
}
