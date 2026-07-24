package com.dororong.rodi.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dororong.rodi.core.ui.R as CoreUiR
import com.dororong.rodi.core.ui.terms.TermsDocument
import com.dororong.rodi.core.ui.terms.TermsDocuments
import com.dororong.rodi.core.ui.terms.TermsWebView
import com.dororong.rodi.core.ui.theme.RodiTheme
import com.dororong.rodi.feature.settings.account.AccountSettingsScreen
import com.dororong.rodi.feature.settings.account.InquiryScreen
import com.dororong.rodi.feature.settings.licenses.OpenSourceLicensesScreen
import com.dororong.rodi.feature.settings.permission.PermissionSettingsScreen
import com.dororong.rodi.feature.settings.terms.TermsReviewScreen

private enum class SettingsDestination {
    Menu,
    Permission,
    Terms,
    Licenses,
    DataSource,
    Account,
    Inquiry,
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    appVersion: String,
    onSessionEnded: () -> Unit,
) {
    var destinationName by rememberSaveable { mutableStateOf(SettingsDestination.Menu.name) }
    var selectedTermsUrl by rememberSaveable { mutableStateOf<String?>(null) }
    val destination = SettingsDestination.valueOf(destinationName)
    val selectedTerms = selectedTermsUrl?.let { url -> TermsDocuments.ALL.firstOrNull { it.url == url } }

    BackHandler {
        when {
            selectedTerms != null -> selectedTermsUrl = null
            destination != SettingsDestination.Menu -> destinationName = SettingsDestination.Menu.name
            else -> onBack()
        }
    }

    when {
        selectedTerms != null -> TermsWebView(
            url = selectedTerms.url,
            modifier = Modifier.fillMaxSize(),
        )

        destination == SettingsDestination.Permission -> PermissionSettingsScreen(
            onBack = { destinationName = SettingsDestination.Menu.name },
        )

        destination == SettingsDestination.Terms -> TermsReviewScreen(
            onBack = { destinationName = SettingsDestination.Menu.name },
            onTermsClick = { selectedTermsUrl = it.url },
        )

        destination == SettingsDestination.Licenses -> OpenSourceLicensesScreen(
            onBack = { destinationName = SettingsDestination.Menu.name },
        )

        destination == SettingsDestination.DataSource -> DataSourceScreen(
            onBack = { destinationName = SettingsDestination.Menu.name },
        )

        destination == SettingsDestination.Account -> AccountSettingsScreen(
            onBack = { destinationName = SettingsDestination.Menu.name },
            onInquiryClick = { destinationName = SettingsDestination.Inquiry.name },
            onSessionEnded = onSessionEnded,
        )

        destination == SettingsDestination.Inquiry -> InquiryScreen(
            onBack = { destinationName = SettingsDestination.Account.name },
        )

        else -> SettingsContent(
            appVersion = appVersion,
            onBack = onBack,
            onPermissionClick = { destinationName = SettingsDestination.Permission.name },
            onTermsClick = { destinationName = SettingsDestination.Terms.name },
            onLicensesClick = { destinationName = SettingsDestination.Licenses.name },
            onDataSourceClick = { destinationName = SettingsDestination.DataSource.name },
            onAccountClick = { destinationName = SettingsDestination.Account.name },
        )
    }
}

@Composable
private fun SettingsContent(
    appVersion: String,
    onBack: () -> Unit,
    onPermissionClick: () -> Unit,
    onTermsClick: () -> Unit,
    onLicensesClick: () -> Unit,
    onDataSourceClick: () -> Unit,
    onAccountClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = RodiTheme.colors.white,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            SettingsTopBar(title = "설정", onBack = onBack)
            Spacer(Modifier.height(12.dp))
            SettingsMenuItem(text = "권한 설정 변경", onClick = onPermissionClick)
            SettingsMenuItem(text = "약관 다시보기", onClick = onTermsClick)
            SettingsMenuItem(text = "오픈소스 라이센스", onClick = onLicensesClick)
            SettingsMenuItem(text = "데이터 출처", onClick = onDataSourceClick)
            SettingsMenuItem(text = "계정정보 관리", onClick = onAccountClick)
            SettingsVersionItem(appVersion = appVersion)
        }
    }
}

@Composable
internal fun SettingsTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            Icon(
                painter = painterResource(CoreUiR.drawable.ic_chevron_left),
                contentDescription = "뒤로",
                tint = RodiTheme.colors.black,
            )
        }
        Text(
            text = title,
            style = RodiTheme.typography.headline1,
            color = RodiTheme.colors.black,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
        )
    }
}

@Composable
private fun SettingsMenuItem(
    text: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(45.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = RodiTheme.typography.body1Medium,
            color = RodiTheme.colors.black,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(CoreUiR.drawable.ic_chevron_right),
            contentDescription = null,
            tint = RodiTheme.colors.gray600,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SettingsVersionItem(appVersion: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(45.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "버전",
            style = RodiTheme.typography.body1Medium,
            color = RodiTheme.colors.black,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = appVersion,
            style = RodiTheme.typography.body1Medium,
            color = RodiTheme.colors.black,
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, widthDp = 375, heightDp = 812)
@Composable
private fun SettingsContentPreview() {
    RodiTheme {
        SettingsContent(
            appVersion = "1.1.0-alpha01",
            onBack = {},
            onPermissionClick = {},
            onTermsClick = {},
            onLicensesClick = {},
            onDataSourceClick = {},
            onAccountClick = {},
        )
    }
}
