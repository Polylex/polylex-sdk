package dev.polylex

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources

/**
 * A [ContextWrapper] that provides [PolylexResources] in place of the default
 * [Resources] instance. Wrapping the activity context here lets every existing
 * `getString(R.string.key)` call route through Polylex without touching call sites.
 *
 * Usage:
 * ```
 * class BaseActivity : AppCompatActivity() {
 *     override fun attachBaseContext(newBase: Context) {
 *         super.attachBaseContext(PolylexContextWrapper.wrap(newBase))
 *     }
 * }
 * ```
 */
public class PolylexContextWrapper private constructor(
    base: Context,
) : ContextWrapper(base) {

    private val polylexResources: Resources by lazy {
        PolylexResources(super.getResources())
    }

    override fun getResources(): Resources = polylexResources

    public companion object {
        @JvmStatic
        public fun wrap(context: Context): ContextWrapper = PolylexContextWrapper(context)
    }
}
