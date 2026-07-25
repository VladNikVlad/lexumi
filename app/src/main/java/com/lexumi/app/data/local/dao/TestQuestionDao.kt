package com.lexumi.app.data.local.dao

import androidx.room.*
import com.lexumi.app.data.local.entity.QuestionOwnerType
import com.lexumi.app.data.local.entity.TestQuestionEntity

@Dao
interface TestQuestionDao {
    @Query("SELECT * FROM test_questions WHERE ownerType = :ownerType AND ownerId = :ownerId")
    suspend fun getForOwner(ownerType: QuestionOwnerType, ownerId: Long): List<TestQuestionEntity>

    @Insert
    suspend fun insert(question: TestQuestionEntity): Long

    @Insert
    suspend fun insertAll(questions: List<TestQuestionEntity>)

    @Delete
    suspend fun delete(question: TestQuestionEntity)
}
