package com.rockthejvm.reviewboard.domain.data

final case class InviteRecord(
    id: Long,
    userEmail: String,
    companyId: Long,
    nInvites: Int,
    active: Boolean = false
)
