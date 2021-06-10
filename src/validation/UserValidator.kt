package xyz.olympusblog.validation

import am.ik.yavi.builder.ValidatorBuilder
import am.ik.yavi.builder.konstraint
import am.ik.yavi.core.Validator
import xyz.olympusblog.models.ChangePasswordDTO
import xyz.olympusblog.models.RegisterDTO
import xyz.olympusblog.models.ResetPasswordDTO
import xyz.olympusblog.models.UpdateUser

val registerValidator: Validator<RegisterDTO> = ValidatorBuilder.of<RegisterDTO>()
    .konstraint(RegisterDTO::username) {
        notNull()
            .greaterThanOrEqual(3)
            .lessThanOrEqual(30)
    }
    .konstraint(RegisterDTO::email) {
        notNull()
            .greaterThanOrEqual(5)
            .lessThanOrEqual(50)
            .email()
    }
    .konstraint(RegisterDTO::password) {
        notNull()
            .greaterThanOrEqual(6)
            .lessThanOrEqual(200)
    }
    .build()

val updateUserValidator: Validator<UpdateUser> = ValidatorBuilder.of<UpdateUser>()
    .konstraint(UpdateUser::username) {
        notNull()
            .greaterThanOrEqual(3)
            .lessThanOrEqual(30)
    }
    .konstraint(UpdateUser::email) {
        notNull()
            .greaterThanOrEqual(5)
            .lessThanOrEqual(50)
            .email()
    }
    .konstraint(UpdateUser::bio) {
        lessThanOrEqual(250)
    }
    .build()

val changePasswordValidator: Validator<ChangePasswordDTO> = ValidatorBuilder.of<ChangePasswordDTO>()
    .konstraint(ChangePasswordDTO::currentPassword) {
        notNull()
            .greaterThanOrEqual(6)
            .lessThanOrEqual(200)
    }
    .konstraint(ChangePasswordDTO::newPassword) {
        notNull()
            .greaterThanOrEqual(5)
            .lessThanOrEqual(50)
    }
    .konstraint(ChangePasswordDTO::confirmNewPassword) {
        notNull()
            .equals(ChangePasswordDTO::newPassword)
    }
    .build()

val resetPasswordValidator: Validator<ResetPasswordDTO> = ValidatorBuilder.of<ResetPasswordDTO>()
    .konstraint(ResetPasswordDTO::token) {
        notNull()
    }
    .konstraint(ResetPasswordDTO::newPassword) {
        notNull()
            .greaterThanOrEqual(5)
            .lessThanOrEqual(50)
    }
    .konstraint(ResetPasswordDTO::confirmNewPassword) {
        notNull()
            .equals(ResetPasswordDTO::newPassword)
    }
    .build()