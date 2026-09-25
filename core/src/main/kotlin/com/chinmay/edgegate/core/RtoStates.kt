package com.chinmay.edgegate.core

/** Human names for plate state codes, used for the "Valid · Karnataka" hint when registering a vehicle. */
object RtoStates {
    private val NAMES = mapOf(
        "AN" to "Andaman & Nicobar", "AP" to "Andhra Pradesh", "AR" to "Arunachal Pradesh", "AS" to "Assam",
        "BR" to "Bihar", "CG" to "Chhattisgarh", "CH" to "Chandigarh", "DD" to "Dadra & Nagar Haveli and Daman & Diu",
        "DL" to "Delhi", "DN" to "Dadra & Nagar Haveli", "GA" to "Goa", "GJ" to "Gujarat", "HP" to "Himachal Pradesh",
        "HR" to "Haryana", "JH" to "Jharkhand", "JK" to "Jammu & Kashmir", "KA" to "Karnataka", "KL" to "Kerala",
        "LA" to "Ladakh", "LD" to "Lakshadweep", "MH" to "Maharashtra", "ML" to "Meghalaya", "MN" to "Manipur",
        "MP" to "Madhya Pradesh", "MZ" to "Mizoram", "NL" to "Nagaland", "OD" to "Odisha", "OR" to "Odisha",
        "PB" to "Punjab", "PY" to "Puducherry", "RJ" to "Rajasthan", "SK" to "Sikkim", "TG" to "Telangana",
        "TN" to "Tamil Nadu", "TR" to "Tripura", "TS" to "Telangana", "UA" to "Uttarakhand", "UK" to "Uttarakhand",
        "UP" to "Uttar Pradesh", "WB" to "West Bengal",
    )

    /** "Karnataka" for a standard plate, "Bharat series" for BH plates, null if unknown. */
    fun describe(candidate: PlateCandidate): String? = when (candidate.format) {
        PlateFormat.BHARAT -> "Bharat series"
        PlateFormat.STANDARD -> NAMES[candidate.plate.take(2)]
    }
}
