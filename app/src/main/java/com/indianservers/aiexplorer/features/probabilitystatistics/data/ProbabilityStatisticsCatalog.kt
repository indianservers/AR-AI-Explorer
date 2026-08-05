package com.indianservers.aiexplorer.features.probabilitystatistics.data

import com.indianservers.aiexplorer.features.probabilitystatistics.models.BundledDataset
import com.indianservers.aiexplorer.features.probabilitystatistics.models.FormulaContent
import com.indianservers.aiexplorer.features.probabilitystatistics.models.PracticeQuestion
import com.indianservers.aiexplorer.features.probabilitystatistics.models.StatisticsCategory
import com.indianservers.aiexplorer.features.probabilitystatistics.models.StatisticsDifficulty
import com.indianservers.aiexplorer.features.probabilitystatistics.models.StatisticsLearningLevel
import com.indianservers.aiexplorer.features.probabilitystatistics.models.StatisticsTopic
import com.indianservers.aiexplorer.features.probabilitystatistics.models.WorkedExample

object ProbabilityStatisticsCatalog {
    private fun topic(
        id: String,
        category: String,
        title: String,
        description: String,
        aliases: Set<String>,
        level: StatisticsLearningLevel,
        difficulty: StatisticsDifficulty,
        prerequisites: List<String>,
        outcomes: List<String>,
        lesson: List<String>,
        formula: FormulaContent,
        example: WorkedExample,
        question: PracticeQuestion,
        applications: List<String>,
        related: List<String>,
    ) = StatisticsTopic(
        id, category, title, description, aliases, level, difficulty, prerequisites,
        outcomes, lesson, listOf(formula), listOf(example), listOf(question), applications, related,
    )

    val topics: List<StatisticsTopic> = listOf(
        topic(
            "mean-median-mode", "descriptive", "Mean, Median and Mode",
            "Use centre measures to describe a typical value while respecting the shape of the data.",
            setOf("average", "central tendency", "middle", "most frequent"),
            StatisticsLearningLevel.Foundation, StatisticsDifficulty.Starter, emptyList(),
            listOf("Calculate three measures of centre", "Choose a measure that resists outliers"),
            listOf("Order the observations.", "Compute the mean from the total.", "Locate the middle value.", "Count repeated values.", "Compare how outliers affect each result."),
            FormulaContent("x̄ = Σxᵢ / n", "x bar equals the sum of all observations divided by their count", listOf("xᵢ" to "each observation", "n" to "number of observations")),
            WorkedExample("Find the centre of 2, 3, 3, 4, 18.", listOf("Sum = 30 and n = 5.", "Mean = 30 / 5 = 6.", "Median = 3; mode = 3.", "The outlier 18 pulls the mean upward."), "Mean 6, median 3, mode 3."),
            PracticeQuestion("Which measure is usually most resistant to one extreme outlier?", listOf("Mean", "Median", "Range", "Variance"), 1, "The median depends on order, so one extreme value usually moves it less than the mean."),
            listOf("Typical salary", "Class performance", "Product delivery time"), listOf("variance-standard-deviation", "data-foundations"),
        ),
        topic(
            "variance-standard-deviation", "descriptive", "Variance and Standard Deviation",
            "Measure how far observations spread around their mean.",
            setOf("spread", "dispersion", "sigma", "standard deviation"),
            StatisticsLearningLevel.School, StatisticsDifficulty.Intermediate, listOf("mean-median-mode"),
            listOf("Distinguish population and sample variance", "Interpret standard deviation in original units"),
            listOf("Find the mean.", "Compute every deviation.", "Square and sum the deviations.", "Divide by N for a population or n−1 for a sample.", "Take the square root for standard deviation."),
            FormulaContent("s² = Σ(xᵢ − x̄)² / (n − 1)", "sample variance equals squared deviations from the mean divided by n minus one", listOf("s²" to "sample variance", "x̄" to "sample mean", "n−1" to "degrees of freedom")),
            WorkedExample("Find the population variance of 2, 4, 6.", listOf("Mean = 4.", "Squared deviations are 4, 0, 4.", "Variance = 8 / 3.", "Standard deviation = √(8/3) ≈ 1.633."), "Variance 2.667; standard deviation 1.633."),
            PracticeQuestion("Why is standard deviation easier to interpret than variance?", listOf("It is always smaller", "It uses original data units", "It ignores outliers", "It equals the range"), 1, "Taking the square root returns the measure to the original units."),
            listOf("Manufacturing consistency", "Investment volatility", "Exam-score spread"), listOf("mean-median-mode", "normal-distribution"),
        ),
        topic(
            "basic-probability", "probability", "Basic Probability",
            "Represent uncertainty with events, sample spaces and probability rules.",
            setOf("chance", "event", "sample space", "odds"),
            StatisticsLearningLevel.Foundation, StatisticsDifficulty.Starter, emptyList(),
            listOf("Build a sample space", "Apply complement and addition rules"),
            listOf("Name the experiment.", "List possible outcomes.", "Define the event.", "Count or estimate favourable outcomes.", "Check that the result lies from 0 to 1."),
            FormulaContent("P(Aᶜ) = 1 − P(A)", "probability of not A equals one minus probability of A", listOf("Aᶜ" to "complement of A", "P(A)" to "probability of A")),
            WorkedExample("A fair die is rolled. Find P(not rolling 6).", listOf("P(6) = 1/6.", "Use the complement rule.", "P(not 6) = 1 − 1/6 = 5/6."), "5/6."),
            PracticeQuestion("If P(rain) = 0.3, what is P(no rain)?", listOf("0.3", "0.7", "1.3", "3.0"), 1, "Complementary probabilities sum to one."),
            listOf("Risk", "Games", "Weather decisions"), listOf("conditional-probability", "binomial-distribution"),
        ),
        topic(
            "conditional-probability", "conditional", "Conditional Probability",
            "Update probability when relevant evidence is known.",
            setOf("given", "bayes", "tree diagram", "diagnostic test"),
            StatisticsLearningLevel.SeniorSecondary, StatisticsDifficulty.Intermediate, listOf("basic-probability"),
            listOf("Calculate conditional probability", "Separate independence from mutual exclusion"),
            listOf("Restrict attention to outcomes in B.", "Find outcomes shared by A and B.", "Divide the intersection probability by P(B).", "Check whether conditioning changed P(A)."),
            FormulaContent("P(A | B) = P(A ∩ B) / P(B)", "probability of A given B equals the intersection divided by probability of B", listOf("A ∩ B" to "both events occur", "P(B)" to "conditioning probability, greater than zero")),
            WorkedExample("60% study, 45% study and pass. Find P(pass | study).", listOf("The conditioning group is students who study.", "Divide 0.45 by 0.60.", "The result is 0.75."), "0.75."),
            PracticeQuestion("When A and B are independent, P(A|B) equals:", listOf("0", "P(B)", "P(A)", "P(A∩B)+1"), 2, "Evidence B does not change the probability of an independent event A."),
            listOf("Medical diagnosis", "Spam filtering", "Quality inspection"), listOf("basic-probability", "bayesian-statistics"),
        ),
        topic(
            "binomial-distribution", "distributions", "Binomial Distribution",
            "Model the number of successes in a fixed sequence of independent yes/no trials.",
            setOf("bernoulli trials", "n choose x", "success count", "binomial"),
            StatisticsLearningLevel.SeniorSecondary, StatisticsDifficulty.Intermediate, listOf("basic-probability", "counting"),
            listOf("Verify binomial conditions", "Compute exact and cumulative probabilities", "Interpret mean and spread"),
            listOf("Confirm a fixed n.", "Confirm two outcomes per trial.", "Keep p constant.", "Require independent trials.", "Let X count successes.", "Combine the number of arrangements with their probabilities."),
            FormulaContent("P(X = x) = C(n,x)pˣ(1−p)ⁿ⁻ˣ", "probability of x successes equals n choose x times p to x times one minus p to n minus x", listOf("n" to "number of trials", "x" to "successes", "p" to "success probability", "C(n,x)" to "ways to position successes")),
            WorkedExample("Find exactly 3 heads in 5 fair tosses.", listOf("n = 5, x = 3, p = 0.5.", "C(5,3) = 10.", "P = 10(0.5)³(0.5)² = 10/32."), "0.3125."),
            PracticeQuestion("Which situation is binomial?", listOf("Cards drawn without replacement", "Number of heads in 20 independent fair tosses", "Daily temperature", "Time until a bus arrives"), 1, "There is a fixed number of independent trials, two outcomes, and constant p."),
            listOf("Defect counts", "Survey responses", "Treatment successes"), listOf("basic-probability", "normal-distribution", "sampling-distributions"),
        ),
        topic(
            "normal-distribution", "distributions", "Normal Distribution",
            "Model symmetric continuous measurements using a centre and spread.",
            setOf("bell curve", "gaussian", "z score", "standard normal"),
            StatisticsLearningLevel.SeniorSecondary, StatisticsDifficulty.Intermediate, listOf("variance-standard-deviation"),
            listOf("Explain μ and σ", "Standardize observations", "Interpret probability as area"),
            listOf("Locate μ at the centre.", "Use σ to control spread.", "Standardize x to a z-score.", "Read probability as area under the density curve."),
            FormulaContent("z = (x − μ) / σ", "z equals observation minus mean divided by standard deviation", listOf("μ" to "population mean", "σ" to "population standard deviation", "z" to "standardized position")),
            WorkedExample("A score is 85 where μ=70 and σ=10.", listOf("Subtract the mean: 85 − 70 = 15.", "Divide by 10.", "z = 1.5."), "The score is 1.5 standard deviations above the mean."),
            PracticeQuestion("Increasing σ while μ stays fixed makes the normal curve:", listOf("Narrower", "Wider", "Move right", "Discrete"), 1, "A larger standard deviation spreads probability over a wider range."),
            listOf("Measurement error", "Test scores", "Manufacturing tolerances"), listOf("variance-standard-deviation", "sampling-distributions"),
        ),
        topic(
            "sampling-distributions", "sampling", "Sampling Distributions",
            "Study how a statistic varies across repeated random samples.",
            setOf("central limit theorem", "standard error", "sample mean", "clt"),
            StatisticsLearningLevel.Undergraduate, StatisticsDifficulty.Advanced, listOf("normal-distribution"),
            listOf("Distinguish data distributions from statistic distributions", "Explain standard error and the CLT"),
            listOf("Define a population.", "Draw many random samples of the same size.", "Calculate one statistic per sample.", "Plot those statistics.", "Compare centre and spread as n changes."),
            FormulaContent("SE(x̄) = σ / √n", "standard error of the mean equals population standard deviation divided by square root of sample size", listOf("SE" to "sampling spread", "σ" to "population spread", "n" to "sample size")),
            WorkedExample("σ=12 and n=36. Find SE.", listOf("√36 = 6.", "SE = 12 / 6."), "2."),
            PracticeQuestion("When sample size quadruples, standard error becomes:", listOf("Four times", "Twice", "Half", "Unchanged"), 2, "SE is proportional to one over the square root of n."),
            listOf("Polling uncertainty", "Quality sampling", "Clinical research"), listOf("confidence-intervals", "hypothesis-testing"),
        ),
        topic(
            "confidence-intervals", "estimation", "Confidence Intervals",
            "Use a sample estimate and its uncertainty to construct a plausible parameter range.",
            setOf("margin of error", "interval estimate", "confidence level"),
            StatisticsLearningLevel.Undergraduate, StatisticsDifficulty.Advanced, listOf("sampling-distributions"),
            listOf("Construct an interval", "Interpret confidence without assigning probability to a fixed parameter"),
            listOf("Choose an estimator.", "Calculate its standard error.", "Choose a justified critical value.", "Compute estimate ± margin.", "Interpret the repeated-sampling method."),
            FormulaContent("estimate ± critical value × SE", "estimate plus or minus critical value times standard error", listOf("SE" to "standard error", "critical value" to "multiplier set by confidence level")),
            WorkedExample("x̄=50, SE=2, 95% z*=1.96.", listOf("Margin = 1.96 × 2 = 3.92.", "Lower = 46.08.", "Upper = 53.92."), "[46.08, 53.92]."),
            PracticeQuestion("A wider confidence level usually produces an interval that is:", listOf("Narrower", "Wider", "Unchanged", "Always invalid"), 1, "Greater confidence needs a larger critical value and margin."),
            listOf("Opinion polls", "Clinical estimates", "Engineering tolerances"), listOf("sampling-distributions", "hypothesis-testing"),
        ),
        topic(
            "hypothesis-testing", "testing", "Hypothesis Testing",
            "Measure how incompatible observed data are with a clearly stated null model.",
            setOf("p value", "null hypothesis", "significance", "type i error"),
            StatisticsLearningLevel.Undergraduate, StatisticsDifficulty.Advanced, listOf("sampling-distributions", "confidence-intervals"),
            listOf("State hypotheses", "Interpret p-values and error risks"),
            listOf("State H₀ and H₁ before inspecting results.", "Choose a statistic and assumptions.", "Calculate evidence under H₀.", "Compare the p-value with α.", "Report effect size and practical context."),
            FormulaContent("test statistic = (estimate − null value) / SE", "test statistic equals estimate minus null value divided by standard error", listOf("H₀" to "null model", "SE" to "standard error", "α" to "chosen Type I error rate")),
            WorkedExample("Estimate=12, H₀ value=10, SE=1.", listOf("Difference = 2.", "Statistic = 2 / 1 = 2.", "Use the reference distribution to obtain a p-value."), "Test statistic 2."),
            PracticeQuestion("A p-value is the probability, assuming H₀, of:", listOf("H₀ being true", "Results at least as extreme as observed", "Making any error", "The effect being important"), 1, "The p-value conditions on the null model and measures extremeness."),
            listOf("A/B testing", "Clinical trials", "Manufacturing claims"), listOf("confidence-intervals", "regression-correlation"),
        ),
        topic(
            "regression-correlation", "regression", "Correlation and Regression",
            "Describe association and model how an outcome changes with predictors.",
            setOf("line of best fit", "pearson", "least squares", "r squared"),
            StatisticsLearningLevel.Undergraduate, StatisticsDifficulty.Advanced, listOf("variance-standard-deviation"),
            listOf("Interpret correlation", "Fit and assess a least-squares line", "Separate association from causation"),
            listOf("Plot paired observations.", "Check shape and unusual points.", "Measure standardized association.", "Fit the least-squares line.", "Inspect residuals and uncertainty.", "Avoid causal claims without a causal design."),
            FormulaContent("ŷ = b₀ + b₁x", "predicted y equals intercept plus slope times x", listOf("b₀" to "intercept", "b₁" to "slope", "ŷ" to "predicted outcome")),
            WorkedExample("A fitted line is ŷ=4+2x. Predict at x=3.", listOf("Substitute x=3.", "ŷ = 4 + 2(3)."), "10."),
            PracticeQuestion("A strong correlation alone proves causation:", listOf("Always", "Only when positive", "Never by itself", "Only for large samples"), 2, "Confounding, reverse direction, and selection can create association without causation."),
            listOf("Demand forecasting", "Dose response", "Sports performance"), listOf("hypothesis-testing", "time-series"),
        ),
        topic(
            "data-foundations", "foundations", "Foundations of Data",
            "Identify populations, samples, variables, measurement scales, bias and data quality.",
            setOf("population", "sample", "variable", "bias", "data cleaning"),
            StatisticsLearningLevel.Foundation, StatisticsDifficulty.Starter, emptyList(),
            listOf("Classify variables", "Identify bias and missingness"),
            listOf("Define the research question.", "Identify observational units.", "Classify each variable.", "Check how the sample was obtained.", "Mark missing, impossible and unusual values."),
            FormulaContent("statistic → sample; parameter → population", "a statistic describes a sample while a parameter describes a population", listOf("statistic" to "sample summary", "parameter" to "population quantity")),
            WorkedExample("A school surveys 100 of 2,000 students.", listOf("Population: all 2,000 students.", "Sample: the surveyed 100.", "A sample mean is a statistic."), "The sample should represent the population."),
            PracticeQuestion("The average age of every citizen is a:", listOf("Statistic", "Parameter", "Sample", "Variable type"), 1, "It summarizes the complete population."),
            listOf("Surveys", "Scientific experiments", "Business records"), listOf("mean-median-mode", "sampling-distributions"),
        ),
        topic(
            "counting", "counting", "Counting Techniques",
            "Count arrangements and selections without exhaustive listing.",
            setOf("permutations", "combinations", "factorial", "pascal triangle"),
            StatisticsLearningLevel.SeniorSecondary, StatisticsDifficulty.Intermediate, emptyList(),
            listOf("Distinguish order-sensitive and order-free counting", "Calculate combinations"),
            listOf("Decide whether order matters.", "Check whether repetition is allowed.", "Apply multiplication across stages.", "Use permutations for ordered choices and combinations for unordered choices."),
            FormulaContent("C(n,r) = n! / [r!(n−r)!]", "n choose r equals n factorial divided by r factorial times n minus r factorial", listOf("n" to "available objects", "r" to "selected objects")),
            WorkedExample("Choose 2 students from 5.", listOf("Order does not matter.", "C(5,2)=5!/(2!3!)."), "10."),
            PracticeQuestion("Selecting a committee usually uses:", listOf("Permutations", "Combinations", "Subtraction", "Integration"), 1, "A committee does not change when its members are listed in another order."),
            listOf("Lottery odds", "Experimental allocation", "Passwords"), listOf("basic-probability", "binomial-distribution"),
        ),
        topic(
            "time-series", "time-series", "Time Series and Forecasting",
            "Separate trend, seasonality and noise to understand ordered observations.",
            setOf("forecast", "moving average", "arima", "seasonality"),
            StatisticsLearningLevel.Undergraduate, StatisticsDifficulty.Advanced, listOf("regression-correlation"),
            listOf("Recognize time dependence", "Calculate forecast errors"),
            listOf("Plot observations in time order.", "Inspect trend and repeating seasonality.", "Choose a baseline forecast.", "Evaluate errors on future observations.", "Update the model as new data arrive."),
            FormulaContent("RMSE = √[Σ(yₜ−ŷₜ)² / n]", "root mean squared error is the square root of average squared forecast errors", listOf("yₜ" to "actual value", "ŷₜ" to "forecast")),
            WorkedExample("Errors are 1, −2, 2.", listOf("Squared errors: 1, 4, 4.", "Mean squared error = 3.", "RMSE = √3."), "RMSE ≈ 1.732."),
            PracticeQuestion("Randomly shuffling time-series rows can destroy:", listOf("Units", "Temporal dependence", "The mean", "Sample size"), 1, "Order carries lag and seasonal information."),
            listOf("Sales forecasting", "Weather monitoring", "Traffic planning"), listOf("regression-correlation", "stochastic"),
        ),
        topic(
            "bayesian-statistics", "bayesian", "Bayesian Statistics",
            "Combine prior information with observed evidence to update uncertainty.",
            setOf("prior", "posterior", "likelihood", "bayes theorem"),
            StatisticsLearningLevel.AdvancedUndergraduate, StatisticsDifficulty.Advanced, listOf("conditional-probability"),
            listOf("Identify prior, likelihood and posterior", "Explain sequential updating"),
            listOf("Choose a prior that represents initial uncertainty.", "Write the data likelihood.", "Multiply prior and likelihood.", "Normalize to obtain the posterior.", "Check sensitivity to reasonable priors."),
            FormulaContent("posterior ∝ likelihood × prior", "posterior is proportional to likelihood times prior", listOf("prior" to "belief before data", "likelihood" to "data model", "posterior" to "updated uncertainty")),
            WorkedExample("A Beta(1,1) prior sees 7 successes and 3 failures.", listOf("Add successes to α.", "Add failures to β.", "Posterior is Beta(8,4)."), "Posterior mean = 8/12."),
            PracticeQuestion("The likelihood is primarily a function of:", listOf("Observed data given parameters", "Future utility only", "The prior only", "Sample labels"), 0, "It measures how the observed data behave under candidate parameter values."),
            listOf("Medical diagnosis", "Adaptive experiments", "Machine learning"), listOf("conditional-probability", "stochastic"),
        ),
    )

    private val representativeTopics = topics.associateBy { it.id }

    val categories: List<StatisticsCategory> = listOf(
        category("foundations", "Foundations of Data", "D", "Populations, variables, measurement, bias and quality", "data-foundations"),
        category("descriptive", "Descriptive Statistics", "x̄", "Centre, spread, position, shape and moments", "mean-median-mode", "variance-standard-deviation"),
        category("visualization", "Data Visualization", "▥", "Histograms, box plots, scatter plots and diagnostic charts", "mean-median-mode"),
        category("counting", "Counting Techniques", "n!", "Permutations, combinations and combinatorial principles", "counting"),
        category("probability", "Foundations of Probability", "P", "Events, sample spaces and probability laws", "basic-probability"),
        category("conditional", "Conditional Probability", "P|", "Evidence, independence, trees and Bayes' theorem", "conditional-probability"),
        category("random-variables", "Random Variables", "X", "PMF, PDF, CDF, expectation and joint behaviour", "basic-probability"),
        category("distributions", "Probability Distributions", "ƒ", "Discrete and continuous distribution families", "binomial-distribution", "normal-distribution"),
        category("sampling", "Sampling and Sampling Distributions", "S", "Sampling designs, error, CLT and resampling", "sampling-distributions"),
        category("estimation", "Statistical Estimation", "CI", "Point estimates, intervals, likelihood and precision", "confidence-intervals"),
        category("testing", "Hypothesis Testing", "H₀", "Evidence, errors, power and common tests", "hypothesis-testing"),
        category("regression", "Correlation and Regression", "r", "Association, prediction and diagnostics", "regression-correlation"),
        category("anova", "Analysis of Variance", "F", "Group variation, factorial designs and post-hoc analysis", "hypothesis-testing"),
        category("nonparametric", "Non-Parametric Statistics", "R", "Rank, permutation and distribution-free methods", "hypothesis-testing"),
        category("time-series", "Time Series and Forecasting", "t", "Trend, seasonality, dependence and forecast accuracy", "time-series"),
        category("multivariate", "Multivariate Statistics", "M", "PCA, clustering, covariance and dimension reduction", "regression-correlation"),
        category("bayesian", "Bayesian Statistics", "B", "Prior, likelihood, posterior and Bayesian decisions", "bayesian-statistics"),
        category("stochastic", "Stochastic Processes", "→", "Random walks, Markov chains and continuous processes", "time-series"),
        category("design", "Experimental Design", "A/B", "Randomization, blocking, power and causal experiments", "hypothesis-testing"),
        category("quality", "Statistical Quality Control", "QC", "Control charts, capability and process variation", "variance-standard-deviation"),
        category("reliability", "Reliability and Survival Analysis", "R(t)", "Failure, hazard, censoring and system reliability", "time-series"),
        category("computing", "Statistical Computing", "∑", "Simulation, resampling, optimization and reproducibility", "sampling-distributions"),
        category("data-lab", "Real-World Data Laboratory", "LAB", "Offline datasets from education, health and business", "data-foundations"),
    )

    private fun category(id: String, title: String, icon: String, description: String, vararg topicIds: String) =
        StatisticsCategory(id, title, icon, description, if (id in setOf("foundations", "descriptive", "visualization", "probability")) StatisticsDifficulty.Starter else StatisticsDifficulty.Intermediate, topicIds.toList())

    val datasets = listOf(
        BundledDataset("student-marks", "Student marks", "Scores from a small classroom assessment.", listOf(54.0, 61.0, 67.0, 67.0, 72.0, 75.0, 78.0, 81.0, 83.0, 88.0, 91.0, 95.0)),
        BundledDataset("daily-temperature", "Daily temperature", "Fourteen offline daily maximum temperatures.", listOf(28.0, 29.0, 31.0, 32.0, 31.0, 30.0, 33.0, 34.0, 35.0, 33.0, 32.0, 31.0, 30.0, 29.0)),
        BundledDataset("weekly-sales", "Weekly sales", "Units sold across twelve weeks.", listOf(120.0, 126.0, 119.0, 138.0, 142.0, 151.0, 149.0, 158.0, 164.0, 171.0, 169.0, 182.0)),
    )

    fun topic(id: String): StatisticsTopic? = representativeTopics[id]

    fun category(id: String): StatisticsCategory? = categories.firstOrNull { it.id == id }

    fun topicsFor(categoryId: String, level: StatisticsLearningLevel): List<StatisticsTopic> =
        category(categoryId)?.topicIds.orEmpty()
            .mapNotNull(::topic)
            .filter { it.minimumLevel.ordinal <= level.ordinal }

    fun search(query: String, level: StatisticsLearningLevel): List<StatisticsTopic> {
        val normalized = query.trim().lowercase()
        val terms = normalized.split(Regex("\\s+")).filter(String::isNotBlank)
        if (terms.isEmpty()) return topics.filter { it.minimumLevel.ordinal <= level.ordinal }
        return topics.filter { topic ->
            val corpus = (listOf(topic.title, topic.shortDescription) + topic.aliases).joinToString(" ").lowercase()
            val words = corpus.split(Regex("[^a-z0-9]+")).filter(String::isNotBlank).toSet()
            topic.minimumLevel.ordinal <= level.ordinal && (
                normalized in corpus ||
                    terms.all { term -> if (term.length == 1) term in words else term in corpus }
                )
        }
    }

    fun nextTopics(topicId: String): List<StatisticsTopic> =
        topic(topicId)?.relatedTopics.orEmpty().mapNotNull(::topic).take(3)
}
