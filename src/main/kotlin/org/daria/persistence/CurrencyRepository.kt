package org.daria.persistence

import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.transaction.Transactional

@ApplicationScoped
class CurrencyRepository(@PersistenceContext var entityManager: EntityManager) {


    @Transactional
    fun save(currencyEntity: CurrencyPairEntity): CurrencyPairEntity {
        try {
            entityManager.persist(currencyEntity)
        } catch (e: Exception) {
            val persistedEntity = entityManager.createQuery(
                "Select c FROM currency_pair c WHERE name = :name",
                CurrencyPairEntity::class.java
            )
                .setParameter("name", currencyEntity.name)
                .resultList

            persistedEntity.get(0).apply {
                buyPrice = currencyEntity.buyPrice
                sellPrice = currencyEntity.sellPrice
                inStock = currencyEntity.inStock
            }.let { entityManager.persist(it) }
        }

        return currencyEntity
    }

    fun findAll(): MutableList<CurrencyPairEntity> {
        val query = entityManager.createQuery("SELECT c FROM currency_pair c", CurrencyPairEntity::class.java)
        return query.resultList
    }
}