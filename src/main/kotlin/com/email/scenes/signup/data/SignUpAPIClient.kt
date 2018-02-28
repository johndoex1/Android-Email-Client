package com.email.scenes.signup.data

import com.email.api.ApiCall
import com.email.api.DuplicateUsernameException
import com.email.api.UnprocessableEntityException
import com.email.db.models.User
import com.github.kittinunf.result.Result
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Created by sebas on 2/26/18.
 */

interface SignUpAPIClient {

    fun createUser(
            user: User,
            password: String,
            recoveryEmail: String?)
            : Result<String, Exception>

    class Default : SignUpAPIClient {
        private val client = OkHttpClient().
                newBuilder().
                connectTimeout(5, TimeUnit.SECONDS).
                readTimeout(5, TimeUnit.SECONDS).
                build()

        override fun createUser(
                user: User,
                password: String,
                recoveryEmail: String?
        ): Result<String, Exception> {
            return Result.of {
                val request = ApiCall.createUser(
                        username = user.nickname,
                        name = user.name,
                        password = password,
                        recoveryEmail = recoveryEmail
                        )
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    response.message()
                } else if(response.code() == 422){
                    throw UnprocessableEntityException(response.code())
                } else {
                    throw DuplicateUsernameException(response.code())
                }
            }
        }
    }
}
