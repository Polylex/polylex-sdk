package dev.polylex.sample

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import dev.polylex.PolylexContextWrapper

/**
 * Wrapping the activity context with [PolylexContextWrapper] is the entire
 * client-side integration: every existing `getString(R.string.X)` call now
 * resolves through Polylex's in-memory cache first, falling back to the
 * bundled `strings.xml` on any miss.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: SampleViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(PolylexContextWrapper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Trigger an initial fetch so the first-cold-start UI shows
        // translations on the second launch (per session-immutability rule).
        viewModel.refresh()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SampleApp(
                        viewModel = viewModel,
                        onPickLocale = ::switchToLocale,
                    )
                }
            }
        }
    }

    private fun switchToLocale(localeTag: String) {
        viewModel.switchToLocale(localeTag) { applied ->
            if (applied) {
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(localeTag),
                )
                recreate()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SampleApp(
    viewModel: SampleViewModel,
    onPickLocale: (String) -> Unit,
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.app_name)) })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            DemoHeader(state = state)
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            SectionTitle(stringResource(R.string.onboarding_welcome_title))
            Text(stringResource(R.string.onboarding_welcome_subtitle))
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.onboarding_cta_get_started))

            Spacer(Modifier.height(24.dp))
            SectionTitle(stringResource(R.string.profile_title))
            Text(stringResource(R.string.home_greeting, "Akhilesh"))
            Text(stringResource(R.string.home_messages_count, 7))
            Text(stringResource(R.string.home_battery_level, 84))
            Text(stringResource(R.string.profile_subtitle))

            Spacer(Modifier.height(24.dp))
            SectionTitle(stringResource(R.string.settings_title))
            Text(stringResource(R.string.settings_language))
            Text(stringResource(R.string.settings_about))
            Text(stringResource(R.string.about_version_label, "0.1.0", 1))

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            Text(stringResource(R.string.demo_pick_language))
            Spacer(Modifier.height(8.dp))
            LocalePicker(onPickLocale = onPickLocale)

            Spacer(Modifier.height(16.dp))
            Button(onClick = { viewModel.refresh(force = true) }) {
                Text(stringResource(R.string.demo_refresh))
            }
        }
    }
}

@Composable
private fun DemoHeader(state: SampleUiState) {
    val text = when (state) {
        SampleUiState.Loading -> stringResource(R.string.demo_status_loading)
        is SampleUiState.Active ->
            stringResource(
                R.string.demo_status_active,
                state.locale,
                state.translationCount,
            )
        is SampleUiState.Error -> stringResource(R.string.demo_status_error, state.message)
    }
    Text(text, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun LocalePicker(onPickLocale: (String) -> Unit) {
    val locales = listOf(
        "en" to "English",
        "hi" to "हिन्दी",
        "ja" to "日本語",
        "fr" to "Français",
        "es" to "Español",
        "ar" to "العربية",
    )
    Column {
        locales.forEach { (tag, label) ->
            OutlinedButton(
                onClick = { onPickLocale(tag) },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text("$label ($tag)")
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}
