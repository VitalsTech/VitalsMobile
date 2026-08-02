package com.vitals.mobile.core.data.users

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsersRepository @Inject constructor(
    private val usersApi: UsersApi,
) {
    suspend fun getUser(publicId: String): UserDto = usersApi.getUser(publicId)
}
