package com.indianservers.aiexplorer.gamifymaths

/** Keep the goal, rules and next action together in every game theme. */
internal fun WorkshopChallenge.playerPrompt(): String {
    if (kind == WorkshopKind.Configurations) {
        val (count, choose, ordered, total) = parameters
        val orderRule = if (ordered == 1)
            "Order matters: AB and BA count as different arrangements."
        else "Order does not matter: AB and BA count as the same team."
        val energyRule = if (mastery > 0)
            " Add the energy numbers on your chosen robots. That total must leave a remainder of ${parameters[5]} when divided by ${parameters[4]}."
        else ""
        return "$prompt Find all $total valid ${if (ordered == 1) "arrangements" else "teams"}. " +
            "\nUse each robot only once in a team. $orderRule$energyRule " +
            "Tap $choose robots, then tap Collect to save them. Make a different team and collect it. When you have all $total, tap Check."
    }
    val action = when (kind) {
        WorkshopKind.Expression -> "Tap number and operation tiles to build a calculation. Use at least ${if (mastery > 0) 2 else 1} operation${if (mastery > 0) "s" else ""}. Tap Run to see its value, then Check to submit."
        WorkshopKind.Pieces -> "Tap pieces to add them to your build. Use Undo to remove the last piece. Tap Check build when you have met all the rules above."
        WorkshopKind.Navigation -> "Tap a move button to move by its shown amounts: across first, then up or down. Use Undo to go back. Tap Land ship when you reach the target."
        WorkshopKind.Balance -> "Choose a number, then an operation to apply it to both sides. Leave one x alone on the left and only a number on the right. Tap Unlock to submit."
        WorkshopKind.Mixture -> "Use the blue and red controls to change each amount. Match both the total and the ratio, then tap Mix batch."
        WorkshopKind.Grid -> "Change Rows and Columns to match the star total and the extra rule above. Tap Launch array to submit."
        WorkshopKind.Machine -> "Choose a number, then an operation to add a step. Steps run from left to right. Make every input produce its shown output using up to 6 steps, then tap Test circuit."
        WorkshopKind.Sort -> "Tap the numbers in the required order. Then tap the placed numbers you need to mark. Tap Verify evidence to submit."
        WorkshopKind.Factors -> "Tap prime numbers to multiply them together. You may use a prime more than once. Match the target, then tap Unlock crypt."
        WorkshopKind.Configurations -> error("Handled above")
    }
    return "$prompt\n$action"
}
