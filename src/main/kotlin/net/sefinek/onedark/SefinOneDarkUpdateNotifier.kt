package net.sefinek.onedark

import com.intellij.ide.BrowserUtil
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
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
import java.util.jar.Manifest

private const val VERSION_PROPERTY = "net.sefinek.one-dark-theme.lastShownVersion"
private const val UPDATES_GROUP_ID = "Sefin One Dark Theme Updates"

class SefinOneDarkUpdateNotifier : ProjectActivity {
    override suspend fun execute(project: Project) {
        val pluginInfo = getCurrentPluginInfo() ?: return
        val currentVersion = pluginInfo.version
        val properties = PropertiesComponent.getInstance()
        val previousVersion = properties.getValue(VERSION_PROPERTY)

        if (previousVersion == currentVersion) {
            return
        }

        properties.setValue(VERSION_PROPERTY, currentVersion)
        val changelogItems = getChangelogItemsForVersion(currentVersion)
        val repositoryUrl = pluginInfo.repositoryUrl
        val changelogUrl = repositoryUrl?.let { "$it/blob/main/CHANGELOG.md" }

        val title =
            if (previousVersion == null) {
                "Sefin One Dark is ready"
            } else {
                "Sefin One Dark has been updated"
            }

        val content =
            if (previousVersion == null) {
                "Thank you for using this theme! Happy coding!"
            } else {
                "The theme has been updated from $previousVersion to $currentVersion. Happy coding!"
            }

        NotificationGroupManager.getInstance()
            .getNotificationGroup(UPDATES_GROUP_ID)
            .createNotification(title, content, NotificationType.INFORMATION)
            .apply {
                if (repositoryUrl != null) {
                    addAction(
                        NotificationAction.createSimple("Open repository") {
                            BrowserUtil.browse(repositoryUrl)
                        },
                    )
                }
            }
            .apply {
                if (changelogItems.isNotEmpty()) {
                    addAction(
                        NotificationAction.createSimple("Show changelog") {
                            ChangelogDialog(project, changelogItems).show()
                        },
                    )
                }
            }
            .apply {
                if (previousVersion == null && changelogUrl != null) {
                    addAction(
                        NotificationAction.createSimple("Open changelog") {
                            BrowserUtil.browse(changelogUrl)
                        },
                    )
                }
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

    private fun getCurrentPluginInfo(): PluginInfo? {
        val manifests = javaClass.classLoader.getResources("META-INF/MANIFEST.MF")

        while (manifests.hasMoreElements()) {
            val manifest =
                manifests.nextElement().openStream().use {
                    Manifest(it)
                }

            val attributes = manifest.mainAttributes
            if (attributes.getValue("Build-Plugin") == "IntelliJ Platform Gradle Plugin") {
                return PluginInfo(
                    version = attributes.getValue("Version") ?: return null,
                    repositoryUrl = attributes.getValue("Repository-Url")?.trim()?.takeIf { it.isNotEmpty() },
                )
            }
        }

        return null
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

private data class PluginInfo(
    val version: String,
    val repositoryUrl: String?,
)

private fun String.escapeHtml(): String =
    replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
