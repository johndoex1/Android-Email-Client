package com.criptext.mail.scenes.emaildetail.workers

import com.criptext.mail.R
import com.criptext.mail.api.HttpClient
import com.criptext.mail.api.PeerEventsApiHandler
import com.criptext.mail.api.ServerErrorException
import com.criptext.mail.bgworker.BackgroundWorker
import com.criptext.mail.bgworker.ProgressReporter
import com.criptext.mail.db.EmailDetailLocalDB
import com.criptext.mail.db.KeyValueStorage
import com.criptext.mail.db.dao.AccountDao
import com.criptext.mail.db.dao.PendingEventDao
import com.criptext.mail.db.models.ActiveAccount
import com.criptext.mail.db.models.EmailLabel
import com.criptext.mail.db.models.Label
import com.criptext.mail.scenes.emaildetail.data.EmailDetailResult
import com.criptext.mail.scenes.label_chooser.SelectedLabels
import com.criptext.mail.scenes.label_chooser.data.LabelWrapper
import com.criptext.mail.utils.ContactUtils
import com.criptext.mail.utils.EmailAddressUtils
import com.criptext.mail.utils.ServerCodes
import com.criptext.mail.utils.UIMessage
import com.criptext.mail.utils.generaldatasource.data.GeneralAPIClient
import com.criptext.mail.utils.peerdata.PeerChangeThreadLabelData
import com.criptext.mail.utils.peerdata.PeerDeleteThreadData
import com.github.kittinunf.result.Result

/**
 * Created by sebas on 04/05/18.
 */

class MoveEmailThreadWorker(
        private val db: EmailDetailLocalDB,
        pendingDao: PendingEventDao,
        private val chosenLabel: String?,
        private val threadId: String,
        private val currentLabel: Label,
        accountDao: AccountDao,
        storage: KeyValueStorage,
        httpClient: HttpClient,
        private val activeAccount: ActiveAccount,
        override val publishFn: (
                EmailDetailResult.MoveEmailThread) -> Unit)
    : BackgroundWorker<EmailDetailResult.MoveEmailThread> {

    private val peerEventHandler = PeerEventsApiHandler.Default(httpClient, activeAccount, pendingDao,
            storage, accountDao)

    private val apiClient = GeneralAPIClient(httpClient, activeAccount.jwt)

    override val canBeParallelized = false


    override fun catchException(ex: Exception): EmailDetailResult.MoveEmailThread =
        if(ex is ServerErrorException) {
            when {
                ex.errorCode == ServerCodes.Unauthorized ->
                    EmailDetailResult.MoveEmailThread.Unauthorized(UIMessage(R.string.device_removed_remotely_exception))
                ex.errorCode == ServerCodes.Forbidden ->
                    EmailDetailResult.MoveEmailThread.Forbidden()
                else -> EmailDetailResult.MoveEmailThread.Failure(
                        message = createErrorMessage(ex),
                        exception = ex)
            }
        }
        else EmailDetailResult.MoveEmailThread.Failure(
                message = createErrorMessage(ex),
                exception = ex)


    override fun work(reporter: ProgressReporter<EmailDetailResult.MoveEmailThread>): EmailDetailResult.MoveEmailThread? {

        val rejectedLabels = Label.defaultItems.rejectedLabelsByMailbox(currentLabel).map { it.id }
        val emailIds = db.getFullEmailsFromThreadId(threadId = threadId, rejectedLabels = rejectedLabels,
                account = activeAccount).map {
            it.email.id
        }

        if(chosenLabel == null){
            val result = Result.of {
                db.deleteRelationByEmailIds(emailIds = emailIds)
                db.deleteThread(threadId, activeAccount)
            }

            return when (result){
                is Result.Success -> {
                    peerEventHandler.enqueueEvent(PeerDeleteThreadData(listOf(threadId)).toJSON())
                    EmailDetailResult.MoveEmailThread.Success(threadId)
                }
                is Result.Failure -> {
                    catchException(result.error)
                }
            }

        }
        val selectedLabels = SelectedLabels()
        selectedLabels.add(LabelWrapper(db.getLabelByName(chosenLabel, activeAccount.id)))

        val selectedLabelsList = selectedLabels.toList().map { it.label }
        val systemLabels = db.getLabelsByName(Label.defaultItems.toList().map { it.text }, activeAccount.id)
                .filter { !rejectedLabels.contains(it.id) }

        val peerSelectedLabels = selectedLabels.toList()
                .filter { it.text != currentLabel.text }
                .toList().map { it.text }
        val peerRemovedLabels = db.getLabelsFromThreadId(threadId)
                .filter { !selectedLabelsList.contains(it) }
                .filter { (!systemLabels.contains(it)) }
                .map { it.text }

        val result = Result.of {
            if(chosenLabel == Label.LABEL_SPAM){
                val fromContacts = db.updateSpamCounter(emailIds, activeAccount.id, activeAccount.userEmail)
                apiClient.postReportSpam(fromContacts, ContactUtils.ContactReportTypes.spam)
            }
            if(chosenLabel == Label.LABEL_TRASH){
                db.setTrashDate(emailIds, activeAccount.id)
            }
            if(currentLabel == Label.defaultItems.trash && chosenLabel == Label.LABEL_SPAM){
                //Mark as spam from trash
                db.deleteRelationByLabelAndEmailIds(labelId = Label.defaultItems.trash.id,
                        emailIds = emailIds, accountId = activeAccount.id)
            }
            val emailLabels = arrayListOf<EmailLabel>()
            emailIds.flatMap{ emailId ->
                selectedLabels.toIDs().map{ labelId ->
                    emailLabels.add(EmailLabel(
                            emailId = emailId,
                            labelId = labelId))
                }
            }
            db.createLabelEmailRelations(emailLabels)
        }

        return when (result) {
            is Result.Success -> {
                peerEventHandler.enqueueEvent(PeerChangeThreadLabelData(listOf(threadId),
                        peerRemovedLabels, peerSelectedLabels).toJSON())
                EmailDetailResult.MoveEmailThread.Success(threadId)
            }
            is Result.Failure -> {
                catchException(result.error)
            }
        }
    }

    override fun cancel() {
    }

    private val createErrorMessage: (ex: Exception) -> UIMessage = { ex ->
        when(ex){
            is ServerErrorException -> UIMessage(resId = R.string.server_bad_status, args = arrayOf(ex.errorCode))
            else -> UIMessage(resId = R.string.unable_to_move_email, args = arrayOf(ex.toString()))
        }
    }
}
