package xyz.olympusblog.validation

import am.ik.yavi.builder.ValidatorBuilder
import am.ik.yavi.builder.konstraint
import am.ik.yavi.core.Validator
import xyz.olympusblog.models.NewArticle
import xyz.olympusblog.models.UpdateArticle

val createArticleValidator: Validator<NewArticle> = ValidatorBuilder.of<NewArticle>()
    .konstraint(NewArticle::title) {
        notNull()
            .greaterThanOrEqual(10)
            .lessThanOrEqual(100)
    }
    .konstraint(NewArticle::description) {
        notNull()
            .greaterThanOrEqual(10)
            .lessThanOrEqual(150)
    }
    .konstraint(NewArticle::body) {
        notNull()
            .notBlank()
    }
    .konstraint(NewArticle::tagList) {
        notNull()
            .greaterThanOrEqual(1)
            .lessThanOrEqual(5)
    }
    .forEach(NewArticle::tagList, "tags") { validatorBuilder ->
        validatorBuilder.constraint(String::toString, "value") {
            it.notNull().lessThanOrEqual(15).greaterThanOrEqual(3)
        }
    }
    .build()

val updateArticleValidator: Validator<UpdateArticle> = ValidatorBuilder.of<UpdateArticle>()
    .konstraint(UpdateArticle::title) {
        greaterThanOrEqual(10)
            .lessThanOrEqual(100)
    }
    .konstraint(UpdateArticle::description) {
        greaterThanOrEqual(10)
            .lessThanOrEqual(150)
    }
    .konstraint(UpdateArticle::body) {
        notEmpty()
    }
    .konstraint(UpdateArticle::tagList) {
        greaterThanOrEqual(1)
            .lessThanOrEqual(5)
    }
    .forEach(UpdateArticle::tagList, "tags") { validatorBuilder ->
        validatorBuilder.constraint(String::toString, "value") {
            it.notNull().lessThanOrEqual(15).greaterThanOrEqual(3)
        }
    }
    .build()