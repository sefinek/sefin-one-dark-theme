package net.sefinek.onedark

import com.intellij.ide.BrowserUtil
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.Dimension
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JEditorPane
import javax.swing.JScrollPane

private const val PLUGIN_ID = "net.sefinek.one-dark-theme"
private const val REPOSITORY_URL = "https://github.com/sefinek/sefin-one-dark-theme"
private const val VERSION_PROPERTY = "net.sefinek.one-dark-theme.lastShownVersion"
private const val UPDATES_GROUP_ID = "Sefin One Dark Theme Updates"

class SefinOneDarkUpdateNotifier : ProjectActivity {
    override suspend fun execute(project: Project) {
        val currentVersion = PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID))?.version ?: return
        val properties = PropertiesComponent.getInstance()
        val previousVersion = properties.getValue(VERSION_PROPERTY)

        if (previousVersion == currentVersion) {
            return
        }

        properties.setValue(VERSION_PROPERTY, currentVersion)
        val isFirstInstall = previousVersion == null

        val title = if (isFirstInstall) "Sefin One Dark is ready" else "Sefin One Dark updated to $currentVersion"
        val content =
            if (isFirstInstall) {
                "Thank you for installing the theme. Select it in Settings > Appearance & Behavior > Appearance."
            } else {
                "The theme has been updated from $previousVersion to $currentVersion."
            }

        val changelogItems = if (isFirstInstall) emptyList() else getChangelogItemsForVersion(currentVersion)

        NotificationGroupManager.getInstance()
            .getNotificationGroup(UPDATES_GROUP_ID)
            .createNotification(title, content, NotificationType.INFORMATION)
            .apply {
                if (changelogItems.isNotEmpty()) {
                    addAction(
                        NotificationAction.createSimple("Show changelog") {
                            ChangelogDialog(project, changelogItems).show()
                        },
                    )
                }

                addAction(
                    NotificationAction.createSimple("Open repository") {
                        BrowserUtil.browse(REPOSITORY_URL)
                    },
                )
            }
            .notify(project)
    }

    private fun getChangelogItemsForVersion(version: String): List<String> {
        val changelog =
            javaClass.getResourceAsStream("/CHANGELOG.md")
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: return emptyList()

        val header = "## [$version]"
        val headerIndex = changelog.indexOf(header)
        if (headerIndex < 0) {
            return emptyList()
        }

        val sectionStart = changelog.indexOf('\n', headerIndex).takeIf { it >= 0 }?.plus(1) ?: return emptyList()
        val sectionEnd = changelog.indexOf("\n## ", sectionStart).takeIf { it >= 0 } ?: changelog.length

        return changelog
            .substring(sectionStart, sectionEnd)
            .lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("- ") }
            .map { it.removePrefix("- ").trim() }
            .toList()
    }

    private class ChangelogDialog(project: Project, changelogItems: List<String>) : DialogWrapper(project) {
        private val changelogHtml =
            changelogItems.joinToString(
                prefix = "<html><body><ul>",
                separator = "",
                postfix = "</ul></body></html>",
            ) { "<li>${it.escapeHtml()}</li>" }

        init {
            title = "What's New in Sefin One Dark"
            init()
        }

        override fun createCenterPanel(): JComponent =
            JScrollPane(
                JEditorPane("text/html", changelogHtml).apply {
                    isEditable = false
                    isOpaque = false
                    font = UIUtil.getLabelFont()
                    foreground = UIUtil.getLabelForeground()
                    border = JBUI.Borders.empty(4, 0)
                    putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
                },
            ).apply {
                preferredSize = Dimension(560, 260)
                border = null
                viewport.isOpaque = false
            }

        override fun createActions(): Array<Action> = arrayOf(okAction)
    }
}

private fun String.escapeHtml(): String =
    replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
