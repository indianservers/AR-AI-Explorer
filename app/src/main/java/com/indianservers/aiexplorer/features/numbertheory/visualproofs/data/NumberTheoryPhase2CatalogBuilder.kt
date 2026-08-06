package com.indianservers.aiexplorer.features.numbertheory.visualproofs.data

import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryParameter
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryPractice
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryProofCategory
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryProofLevel
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryProofStep
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryVisualModel
import com.indianservers.aiexplorer.features.numbertheory.visualproofs.domain.NumberTheoryVisualProofTopic

internal fun phase2Topic(
    id: String,
    category: NumberTheoryProofCategory,
    title: String,
    aliases: Set<String>,
    level: NumberTheoryProofLevel,
    visualModel: NumberTheoryVisualModel,
    statement: String,
    spokenStatement: String,
    parameters: List<NumberTheoryParameter>,
    steps: List<Triple<String, String, String>>,
    discoveryQuestion: String,
    why: List<String>,
    mistake: String,
    practice: NumberTheoryPractice,
) = NumberTheoryVisualProofTopic(
    id = id,
    category = category,
    title = title,
    aliases = aliases,
    level = level,
    visualModel = visualModel,
    statement = statement,
    spokenStatement = spokenStatement,
    parameters = parameters,
    steps = steps.mapIndexed { index, row ->
        NumberTheoryProofStep(
            id = "step-${index + 1}",
            instruction = row.first,
            observation = row.second,
            expression = row.third,
            spokenExpression = row.third
                .replace("×", " times ")
                .replace("÷", " divided by ")
                .replace("²", " squared ")
                .replace("≡", " is congruent to "),
        )
    },
    discoveryQuestion = discoveryQuestion,
    whyItWorks = why,
    commonMistake = mistake,
    practice = practice,
    completedInPhase = 2,
)

internal fun phase2Practice(
    prompt: String,
    options: List<String>,
    answer: Int,
    explanation: String,
) = NumberTheoryPractice(prompt, options, answer, explanation)
