package xyz.olympusblog.validation

import am.ik.yavi.builder.ValidatorBuilder
import am.ik.yavi.builder.konstraint
import am.ik.yavi.core.Validator
import xyz.olympusblog.models.CommentDTO

val createCommentValidator: Validator<CommentDTO> = ValidatorBuilder.of<CommentDTO>()
    .konstraint(CommentDTO::body) {
        notNull()
            .greaterThanOrEqual(3)
            .lessThanOrEqual(250)
    }
    .build()