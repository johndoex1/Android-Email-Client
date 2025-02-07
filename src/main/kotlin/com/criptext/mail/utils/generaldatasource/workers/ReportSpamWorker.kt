package com.criptext.mail.utils.generaldatasource.workers

import com.criptext.mail.R
import com.criptext.mail.api.HttpClient
import com.criptext.mail.api.ServerErrorException
import com.criptext.mail.bgworker.BackgroundWorker
import com.criptext.mail.bgworker.ProgressReporter
import com.criptext.mail.db.models.ActiveAccount
import com.criptext.mail.utils.ContactUtils
import com.criptext.mail.utils.ServerCodes
import com.criptext.mail.utils.UIMessage
import com.criptext.mail.utils.generaldatasource.data.GeneralAPIClient
import com.criptext.mail.utils.generaldatasource.data.GeneralResult
import com.github.kittinunf.result.Result
import org.json.JSONObject


class ReportSpamWorker(val httpClient: HttpClient,
                       val email: List<String>,
                       val type: ContactUtils.ContactReportTypes,
                       activeAccount: ActiveAccount,
                       override val publishFn: (GeneralResult) -> Unit)
    : BackgroundWorker<GeneralResult.ReportSpam> {

    private val apiClient = GeneralAPIClient(httpClient, activeAccount.jwt)

    override val canBeParallelized = true

    override fun catchException(ex: Exception): GeneralResult.ReportSpam {
        return GeneralResult.ReportSpam.Failure(createErrorMessage(ex))
    }

    override fun work(reporter: ProgressReporter<GeneralResult.ReportSpam>): GeneralResult.ReportSpam? {
        val result = Result.of { apiClient.postReportSpam(email, type) }

        return when (result) {
            is Result.Success -> GeneralResult.ReportSpam.Success()
            is Result.Failure -> catchException(result.error)
        }

    }

    override fun cancel() {
        TODO("not implemented") //To change body of created functions use CRFile | Settings | CRFile Templates.
    }

    private val createErrorMessage: (ex: Exception) -> UIMessage = { ex ->
        when (ex) {
            is ServerErrorException ->
                if(ex.errorCode == ServerCodes.BadRequest)
                UIMessage(resId = R.string.forgot_password_error_400)
                else
                    UIMessage(resId = R.string.server_bad_status, args = arrayOf(ex.errorCode))
            else ->UIMessage(resId = R.string.forgot_password_error)
        }
    }

}