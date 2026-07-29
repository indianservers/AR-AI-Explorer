package com.indianservers.aiexplorer.chemistry.formulas.data

import com.indianservers.aiexplorer.chemistry.formulas.model.*

object BundledChemistryFormulaData {
    const val SCHEMA_VERSION = 2

    private data class Seed(val id: String, val title: String, val description: String, val topics: List<String>)

    private fun topics(value: String) = value.split(';').map(String::trim)

    private val seeds = listOf(
        Seed("foundations-stoichiometry", "Foundations and Stoichiometry", "Moles, composition, equations and reaction yield.", topics("Mole Concept;Molar Mass;Percentage Composition;Empirical Formula;Molecular Formula;Balanced Equations;Mole Ratios;Limiting Reagent;Percentage Yield")),
        Seed("atomic-electronic", "Atomic and Electronic Structure", "Atomic particles, radiation and electron configuration.", topics("Atomic Composition;Average Atomic Mass;Photon Energy;Frequency and Wavelength;de Broglie Relation;Bohr Model;Quantum Numbers;Electron Capacity;Effective Nuclear Charge")),
        Seed("periodic-bonding", "Periodic Properties and Bonding", "Trends, Lewis structures and molecular geometry.", topics("Periodic Trends;Formal Charge;Bond Order;Dipole Moment;VSEPR;Hybridisation;Molecular Orbital Theory;Lattice Energy")),
        Seed("states-gases", "States of Matter and Gas Laws", "Gases, liquids, solids and density relationships.", topics("Density;Gas Pressure;Boyle's Law;Charles's Law;Combined Gas Law;Ideal Gas Law;Dalton's Law;Graham's Law;Real Gases;Crystal Density")),
        Seed("solutions-colligative", "Solutions and Colligative Properties", "Concentration measures and dilute-solution effects.", topics("Molarity;Molality;Normality;Mole Fraction;Dilution;Solubility;Osmotic Pressure;Boiling-point Elevation;Freezing-point Depression")),
        Seed("thermal-thermodynamics", "Thermochemistry and Thermodynamics", "Heat, enthalpy, entropy and spontaneity.", topics("Specific Heat;Calorimetry;Enthalpy;Hess's Law;Bond Enthalpy;Entropy;Gibbs Free Energy;Equilibrium Connection")),
        Seed("equilibrium", "Chemical and Ionic Equilibrium", "Equilibrium constants, reaction quotients and solubility.", topics("Equilibrium Constant;Reaction Quotient;Kp and Kc;Degree of Dissociation;Solubility Product;Common-ion Effect;Salt Hydrolysis")),
        Seed("acids-bases", "Acids, Bases and Buffers", "pH, pOH, weak acid/base and buffer calculations.", topics("pH;pOH;Ionic Product of Water;Strong Acids;Strong Bases;Weak Acids;Weak Bases;Buffer Systems;Acid-base Indicators")),
        Seed("kinetics", "Chemical Kinetics", "Rates, orders, integrated laws and activation energy.", topics("Reaction Rate;Rate Law;Rate Constant;Reaction Order;Integrated Rate Laws;Half-life;Arrhenius Equation;Activation Energy")),
        Seed("electrochemistry-redox", "Electrochemistry and Redox", "Cell potential, electrolysis and oxidation-state tools.", topics("Oxidation State;Redox Balancing;Cell Potential;Nernst Equation;Gibbs Energy;Electrolysis;Faraday's Laws;Conductivity;Batteries")),
        Seed("analytical-spectroscopy", "Analytical Chemistry and Spectroscopy", "Measurement, statistics, titration and instrument laws.", topics("Titration;Gravimetric Analysis;Calibration Curves;Error Analysis;Statistics;Beer-Lambert Law;Infrared Spectroscopy;NMR Foundations;Mass Spectrometry")),
        Seed("organic-biochemistry", "Organic and Biochemistry Calculations", "Organic formula logic and biological chemistry models.", topics("General Formulas;Degree of Unsaturation;Reaction Yield;Optical Rotation;Acid-base Relationships;Enzyme Kinetics;Protein Concentration;Osmolarity")),
        Seed("inorganic-solid", "Inorganic, Coordination and Solid State", "Coordination chemistry, crystals and solid-state relations.", topics("Coordination Number;Effective Atomic Number;Crystal Field Splitting;Magnetic Moment;Stability Constants;Unit Cells;Packing Efficiency;Bragg's Law;Crystal Defects")),
        Seed("advanced-physical", "Advanced Physical Chemistry", "Higher-level physical chemistry models.", topics("Quantum Chemistry;Statistical Thermodynamics;Molecular Spectroscopy;Transport Properties;Phase Equilibria;Fugacity;Activity;Chemical Potential"))
    )

    val catalogue: ChemistryFormulaCatalogue by lazy {
        val subcategories = seeds.flatMap { seed ->
            seed.topics.map { title ->
                ChemistryFormulaSubcategory(
                    id = "${seed.id}-${slug(title)}",
                    categoryId = seed.id,
                    title = title,
                    description = "Quantitative relationships, notation and validity conditions for $title."
                )
            }
        }
        val categories = seeds.map { seed ->
            ChemistryFormulaCategory(
                id = seed.id,
                title = seed.title,
                description = seed.description,
                subcategoryIds = subcategories.filter { it.categoryId == seed.id }.map { it.id }
            )
        }
        ChemistryFormulaCatalogue(SCHEMA_VERSION, categories, subcategories, formulas(subcategories))
    }

    private fun formulas(subcategories: List<ChemistryFormulaSubcategory>): List<ChemistryFormula> {
        fun f(
            id: String,
            category: String,
            subcategory: String,
            title: String,
            equation: String,
            spoken: String,
            level: ChemistryFormulaLevel,
            vars: String,
            example: String,
            calculator: Boolean = true,
            derivation: Boolean = true,
            keywords: String = title
        ): ChemistryFormula {
            val subId = subcategories.first { it.categoryId == category && it.title == subcategory }.id
            val variables = vars.split(';').filter(String::isNotBlank).map { spec ->
                spec.split('|').let {
                    ChemistryFormulaVariable(it[0], it[1], it[2], it[2], it.getOrNull(3)?.ifBlank { null }, it.getOrNull(4)?.ifBlank { null })
                }
            }
            val searchable = equation
                .replace("\\frac", "")
                .replace("\\sqrt", "sqrt")
                .replace("\\Delta", "delta")
                .replace("\\nu", "nu")
                .replace("\\theta", "theta")
                .replace("\\mu", "mu")
            return ChemistryFormula(
                id = id,
                categoryId = category,
                subcategoryId = subId,
                title = title,
                equation = equation,
                searchableEquation = searchable,
                spokenEquation = spoken,
                description = "Use this LaTeX-style relationship with balanced chemistry, compatible units and the stated temperature or standard-state convention.",
                minimumLevel = level,
                variables = variables,
                assumptions = listOf("Amounts and units are consistent; temperature is in kelvin when required."),
                limitations = listOf("Approximations, activities and standard-state choices must be checked before use."),
                alternativeForms = emptyList(),
                derivationSteps = if (derivation) listOf(ChemistryDerivationStep(equation, "This form follows from the stated chemical definition or model.")) else emptyList(),
                workedExamples = listOf(ChemistryWorkedExample(example, "Convert inputs to compatible units and substitute.", "Evaluate with appropriate significant figures and chemical units.", "The quantities and units are consistent with the defining relationship.")),
                unitCheck = "Units on both sides are compatible with the stated chemical quantity.",
                calculator = if (calculator) ChemistryCalculatorDefinition(variables.map { it.id }.toSet(), variables.map { it.id }.toSet()) else null,
                relatedFormulaIds = emptyList(),
                relatedConceptIds = emptyList(),
                keywords = (keywords.lowercase().split(Regex("[^a-z0-9]+")) + title.lowercase().split(' ')).filter { it.isNotBlank() }.toSet(),
                featured = level.rank <= ChemistryFormulaLevel.Class10.rank
            )
        }

        return listOf(
            f("chem-moles", "foundations-stoichiometry", "Mole Concept", "Amount of substance", "n = \\frac{m}{M}", "amount in moles equals mass divided by molar mass", ChemistryFormulaLevel.Class9, "amount|n|amount of substance|mol|[N];mass|m|sample mass|g|[M];molarMass|M|molar mass|g/mol|[M N^-1]", "Find moles in 18.0 g of water with molar mass 18.0 g/mol.", keywords = "moles mole concept molar mass"),
            f("chem-mole-ratio", "foundations-stoichiometry", "Mole Ratios", "Stoichiometric mole ratio", "\\frac{n_A}{a} = \\frac{n_B}{b}", "moles of A divided by its coefficient equals moles of B divided by its coefficient", ChemistryFormulaLevel.Class10, "amountA|n_A|amount of A|mol|[N];coefficientA|a|coefficient of A||1;amountB|n_B|amount of B|mol|[N];coefficientB|b|coefficient of B||1", "Use a balanced equation to convert reactant moles to product moles."),
            f("chem-photon-energy", "atomic-electronic", "Photon Energy", "Photon energy", "E = h\\nu", "photon energy equals Planck constant multiplied by frequency", ChemistryFormulaLevel.Class11, "energy|E|photon energy|J|[M L^2 T^-2];frequency|nu|frequency|Hz|[T^-1]", "Find the energy of radiation of known frequency."),
            f("chem-electron-capacity", "atomic-electronic", "Electron Capacity", "Maximum shell capacity", "N = 2n^2", "maximum shell population equals two times principal quantum number squared", ChemistryFormulaLevel.Class11, "capacity|N|maximum electrons||1;shell|n|principal quantum number||1", "Find the maximum electron capacity of the third shell."),
            f("chem-effective-charge", "atomic-electronic", "Effective Nuclear Charge", "Simple effective nuclear charge model", "Z_{eff} \\approx Z - S", "effective nuclear charge is approximately nuclear charge minus screening constant", ChemistryFormulaLevel.Undergraduate, "effective|Z_eff|effective nuclear charge||1;atomic|Z|atomic number||1;screening|S|screening constant||1", "Estimate effective nuclear charge using a stated screening model.", calculator = false, keywords = "periodic effective nuclear charge screening approximate model"),
            f("chem-formal-charge", "periodic-bonding", "Formal Charge", "Formal charge", "FC = V - N - \\frac{B}{2}", "formal charge equals valence electrons minus nonbonding electrons minus half bonding electrons", ChemistryFormulaLevel.Class11, "charge|FC|formal charge||1;valence|V|valence electrons||1;nonbonding|N|nonbonding electrons||1;bonding|B|bonding electrons||1", "Calculate formal charge for an atom in a Lewis structure."),
            f("chem-density", "states-gases", "Density", "Density", "\\rho = \\frac{m}{V}", "density equals mass divided by volume", ChemistryFormulaLevel.Class8, "density|rho|density|kg/m^3|[M L^-3];mass|m|mass|kg|[M];volume|V|volume|m^3|[L^3]", "Find density from 2.0 g occupying 1.0 mL."),
            f("chem-ideal-gas", "states-gases", "Ideal Gas Law", "Ideal gas law", "PV = nRT", "pressure times volume equals amount times gas constant times absolute temperature", ChemistryFormulaLevel.Class11, "pressure|P|pressure|Pa|[M L^-1 T^-2];volume|V|volume|m^3|[L^3];amount|n|amount|mol|[N];temperature|T|absolute temperature|K|[K]", "Find gas volume from pressure, amount and kelvin temperature.", keywords = "ideal gas pv nrt gas law pressure volume"),
            f("chem-molarity", "solutions-colligative", "Molarity", "Molar concentration", "c = \\frac{n}{V}", "molar concentration equals amount divided by solution volume", ChemistryFormulaLevel.Class10, "concentration|c|molar concentration|mol/L|[N L^-3];amount|n|solute amount|mol|[N];volume|V|solution volume|L|[L^3]", "Dissolve 0.50 mol to make 2.0 L solution.", keywords = "molarity concentration moles volume"),
            f("chem-osmotic-pressure", "solutions-colligative", "Osmotic Pressure", "Osmotic pressure", "\\Pi = icRT", "osmotic pressure equals van 't Hoff factor times concentration times gas constant times temperature", ChemistryFormulaLevel.Class12, "pressure|Pi|osmotic pressure|Pa|[M L^-1 T^-2];factor|i|van 't Hoff factor||1;concentration|c|amount concentration|mol/m^3|[N L^-3];temperature|T|temperature|K|[K]", "Find osmotic pressure of a dilute solution at known temperature."),
            f("chem-calorimetry", "thermal-thermodynamics", "Specific Heat", "Heat transfer", "q = mc\\Delta T", "heat equals mass times specific heat capacity times temperature change", ChemistryFormulaLevel.Class10, "heat|q|heat|J|[M L^2 T^-2];mass|m|mass|kg|[M];capacity|c|specific heat capacity|J/(kg K)|[L^2 T^-2 K^-1];temperature|Delta T|temperature change|K|[K]", "Calculate heat for a sample warmed by a known temperature interval."),
            f("chem-gibbs", "thermal-thermodynamics", "Gibbs Free Energy", "Gibbs free-energy change", "\\Delta G = \\Delta H - T\\Delta S", "Gibbs energy change equals enthalpy change minus temperature times entropy change", ChemistryFormulaLevel.Class12, "gibbs|Delta G|Gibbs energy change|J/mol|[M L^2 T^-2 N^-1];enthalpy|Delta H|enthalpy change|J/mol|[M L^2 T^-2 N^-1];temperature|T|temperature|K|[K];entropy|Delta S|entropy change|J/(mol K)|[M L^2 T^-2 N^-1 K^-1]", "Evaluate spontaneity from enthalpy and entropy changes at a stated temperature."),
            f("chem-equilibrium-constant", "equilibrium", "Equilibrium Constant", "Concentration equilibrium constant", "K_c = \\frac{\\prod[products]^\\nu}{\\prod[reactants]^\\nu}", "K c equals product activities over reactant activities raised to stoichiometric coefficients", ChemistryFormulaLevel.Class11, "constant|K_c|equilibrium constant||1;coefficient|nu|stoichiometric coefficient||1", "Construct Kc from a balanced homogeneous reaction.", calculator = false, keywords = "equilibrium kc reaction quotient products reactants"),
            f("chem-ksp", "equilibrium", "Solubility Product", "Solubility product", "K_{sp} = \\prod a_i^{\\nu_i}", "solubility product equals ion activities raised to stoichiometric powers", ChemistryFormulaLevel.Class12, "constant|K_sp|solubility product||1;activity|a_i|ion activity||1;coefficient|nu_i|stoichiometric coefficient||1", "Write Ksp for a sparingly soluble salt.", calculator = false),
            f("chem-ph", "acids-bases", "pH", "pH", "pH = -\\log_{10}a(H^+)", "p H equals negative base ten logarithm of hydrogen ion activity", ChemistryFormulaLevel.Class10, "ph|pH|pH||1;activity|a(H+)|hydrogen ion activity||1", "Find pH for hydrogen-ion activity 1.0 x 10^-3.", keywords = "ph acid hydrogen concentration log"),
            f("chem-first-order", "kinetics", "Integrated Rate Laws", "First-order integrated rate law", "\\ln\\left(\\frac{[A]_0}{[A]_t}\\right) = kt", "natural log of initial concentration over concentration at time t equals rate constant times time", ChemistryFormulaLevel.Class12, "initial|[A]_0|initial concentration|mol/L|[N L^-3];remaining|[A]_t|concentration at time t|mol/L|[N L^-3];constant|k|first-order rate constant|s^-1|[T^-1];time|t|time|s|[T]", "Find concentration remaining for a known first-order rate constant."),
            f("chem-nernst", "electrochemistry-redox", "Nernst Equation", "Nernst equation", "E = E^\\circ - \\frac{RT}{nF}\\ln Q", "cell potential equals standard potential minus R T over electron number and Faraday constant times natural log reaction quotient", ChemistryFormulaLevel.Class12, "potential|E|cell potential|V|[V];standard|E^circ|standard cell potential|V|[V];temperature|T|temperature|K|[K];electrons|n|electrons transferred||1;quotient|Q|reaction quotient||1", "Calculate a non-standard cell potential from standard potential, temperature and Q.", keywords = "nernst electrochemistry cell potential electrode"),
            f("chem-beer-lambert", "analytical-spectroscopy", "Beer-Lambert Law", "Beer-Lambert law", "A = \\varepsilon bc", "absorbance equals molar absorptivity times path length times concentration", ChemistryFormulaLevel.Class12, "absorbance|A|absorbance|AU|1;absorptivity|epsilon|molar absorptivity|L/(mol cm)|;path|b|path length|cm|[L];concentration|c|concentration|mol/L|[N L^-3]", "Find concentration from absorbance, path length and absorptivity.", keywords = "beer lambert absorbance spectroscopy concentration"),
            f("chem-standard-deviation", "analytical-spectroscopy", "Statistics", "Sample standard deviation", "s = \\sqrt{\\frac{\\sum(x_i - \\bar{x})^2}{n - 1}}", "sample standard deviation is square root of squared deviations divided by n minus one", ChemistryFormulaLevel.Undergraduate, "deviation|s|sample standard deviation||;observation|x_i|observation||;mean|x_bar|sample mean||;count|n|sample count||1", "Calculate precision from replicate analytical measurements.", calculator = false),
            f("chem-unsaturation", "organic-biochemistry", "Degree of Unsaturation", "Degree of unsaturation", "DBE = C - \\frac{H}{2} + \\frac{N}{2} + 1", "double bond equivalents equal carbon count minus half hydrogen count plus half nitrogen count plus one", ChemistryFormulaLevel.Class12, "dbe|DBE|double bond equivalents||1;carbon|C|carbon atoms||1;hydrogen|H|hydrogen plus halogen atoms||1;nitrogen|N|nitrogen atoms||1", "Find DBE for a neutral closed-shell organic molecular formula."),
            f("chem-michaelis-menten", "organic-biochemistry", "Enzyme Kinetics", "Michaelis-Menten equation", "v = \\frac{V_{max}[S]}{K_m + [S]}", "rate equals maximum rate times substrate concentration divided by K m plus substrate concentration", ChemistryFormulaLevel.Undergraduate, "rate|v|reaction rate|mol/(L s)|[N L^-3 T^-1];maximum|V_max|maximum rate|mol/(L s)|[N L^-3 T^-1];substrate|[S]|substrate concentration|mol/L|[N L^-3];constant|K_m|Michaelis constant|mol/L|[N L^-3]", "Find enzyme rate at a specified substrate concentration."),
            f("chem-magnetic-moment", "inorganic-solid", "Magnetic Moment", "Spin-only magnetic moment", "\\mu = \\sqrt{n(n + 2)}\\ \\mu_B", "magnetic moment equals square root of n times n plus two Bohr magnetons", ChemistryFormulaLevel.Undergraduate, "moment|mu|magnetic moment|muB|;unpaired|n|unpaired electrons||1", "Estimate spin-only moment from the number of unpaired electrons."),
            f("chem-unit-cell-density", "inorganic-solid", "Unit Cells", "Unit-cell density", "\\rho = \\frac{ZM}{N_Aa^3}", "crystal density equals formula units times molar mass divided by Avogadro constant times cell edge cubed", ChemistryFormulaLevel.Class12, "density|rho|crystal density|kg/m^3|[M L^-3];units|Z|formula units per cell||1;molarMass|M|molar mass|kg/mol|[M N^-1];edge|a|cubic cell edge|m|[L]", "Find cubic-crystal density from unit-cell data."),
            f("chem-chemical-potential", "advanced-physical", "Chemical Potential", "Chemical potential", "\\mu = \\mu^\\circ + RT\\ln a", "chemical potential equals standard chemical potential plus R T times natural log activity", ChemistryFormulaLevel.Undergraduate, "potential|mu|chemical potential|J/mol|[M L^2 T^-2 N^-1];standard|mu^circ|standard chemical potential|J/mol|[M L^2 T^-2 N^-1];temperature|T|temperature|K|[K];activity|a|activity||1", "Find chemical potential relative to a defined standard state.", keywords = "advanced physical chemistry activity chemical potential")
        )
    }

    private fun slug(value: String) = value.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
}
