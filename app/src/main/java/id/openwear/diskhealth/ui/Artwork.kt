package id.openwear.diskhealth.ui

import id.openwear.diskhealth.R
import java.util.Locale

/**
 * Local replacement for the v4.0.1 remote artwork map (`p051t1.a`).
 *
 * The original downloaded a PNG per device codename from i.ibb.co. This clone
 * bundles the same set under `drawable-nodpi` so it works offline.
 */
object Artwork {

    private val devices = listOf(
        "alioth" to R.drawable.device_alioth,
        "brmable" to R.drawable.device_brmable,
        "curtana" to R.drawable.device_curtana,
        "flame" to R.drawable.device_flame,
        "ginkgo" to R.drawable.device_ginkgo,
        "hlte" to R.drawable.device_hlte,
        "juice" to R.drawable.device_juice,
        "lavender" to R.drawable.device_lavender,
        "meizu m6" to R.drawable.device_meizu_m6,
        "mido" to R.drawable.device_mido,
        "oneplus11" to R.drawable.device_oneplus11,
        "pixel1" to R.drawable.device_pixel1,
        "redfin" to R.drawable.device_redfin,
        "rog2" to R.drawable.device_rog2,
        "sargo" to R.drawable.device_sargo,
        "sirius" to R.drawable.device_sirius,
        "spes" to R.drawable.device_spes,
        "star" to R.drawable.device_star,
        "tulip" to R.drawable.device_tulip,
        "vayu" to R.drawable.device_vayu2,
        "whyred" to R.drawable.device_whyred
    )

    fun forModel(model: String): Int {
        val key = model.lowercase(Locale.ROOT)
        return devices.firstOrNull { (name, _) -> key.contains(name) }?.second
            ?: R.drawable.device_unknown
    }
}
