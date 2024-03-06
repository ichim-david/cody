package com.sourcegraph.cody.config

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.ui.awt.RelativePoint
import com.sourcegraph.cody.auth.ui.AccountsListModel
import com.sourcegraph.cody.auth.ui.AccountsListModelBase
import javax.swing.JComponent

class CodyAccountListModel(private val project: Project) :
    AccountsListModelBase<CodyAccount, String>(),
    AccountsListModel.WithActive<CodyAccount, String>,
    CodyAccountsHost {

  private val actionManager = ActionManager.getInstance()

  override var activeAccount: CodyAccount? = null

  override fun addAccount(parentComponent: JComponent, point: RelativePoint?) {
    val group = actionManager.getAction("cody.accounts.addAccount") as ActionGroup
    val popup = actionManager.createActionPopupMenu("LogInToSourcegraphAction", group)

    val actualPoint = point ?: RelativePoint.getCenterOf(parentComponent)
    popup.setTargetComponent(parentComponent)
    JBPopupMenu.showAt(actualPoint, popup.component)
  }

  override fun editAccount(parentComponent: JComponent, account: CodyAccount) {

    val token = newCredentials[account] ?: getOldToken(account)
    val authData =
        CodyAuthenticationManager.instance.login(
            project,
            parentComponent,
            CodyLoginRequest(
                login = account.name,
                server = account.server,
                token = token,
                customRequestHeaders = account.server.customRequestHeaders,
                title = "Edit Sourcegraph Account",
                loginButtonText = "Save account",
            ))

    if (authData == null) return

    account.name = authData.login
    account.server.url = authData.server.url
    account.server.customRequestHeaders = authData.server.customRequestHeaders
    newCredentials[account] = authData.token
    notifyCredentialsChanged(account)
  }

  private fun getOldToken(account: CodyAccount) =
      CodyAuthenticationManager.instance.getTokenForAccount(account)

  override fun addAccount(
      server: SourcegraphServerPath,
      login: String,
      displayName: String?,
      token: String,
      id: String
  ) {
    val account = CodyAccount.create(login, displayName, server, id)
    if (accountsListModel.isEmpty) {
      activeAccount = account
    }
    accountsListModel.add(account)
    newCredentials[account] = token
    notifyCredentialsChanged(account)
  }

  override fun isAccountUnique(login: String, server: SourcegraphServerPath): Boolean =
      accountsListModel.items.none { it.name == login && it.server.url == server.url }
}
