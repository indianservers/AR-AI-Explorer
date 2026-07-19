package com.indianservers.aiexplorer.chemistry.formulas.units

enum class ChemistryUnitDimension { Mass, Amount, Volume, Concentration, Pressure, Temperature, Energy, Enthalpy, Entropy, Rate, RateConstant, Conductivity, MolarConductivity, Absorbance, Wavelength, Frequency, ElectrodePotential, Dimensionless }
data class ChemistryUnit(val symbol: String, val name: String, val dimension: ChemistryUnitDimension, val scaleToSi: Double = 1.0, val offsetToSi: Double = 0.0) { fun toSi(value: Double) = value * scaleToSi + offsetToSi; fun fromSi(value: Double) = (value - offsetToSi) / scaleToSi }
object ChemistryUnitSystem {
    val units = listOf(
        ChemistryUnit("kg","kilogram",ChemistryUnitDimension.Mass), ChemistryUnit("g","gram",ChemistryUnitDimension.Mass,.001), ChemistryUnit("mg","milligram",ChemistryUnitDimension.Mass,1e-6),
        ChemistryUnit("mol","mole",ChemistryUnitDimension.Amount), ChemistryUnit("m³","cubic metre",ChemistryUnitDimension.Volume), ChemistryUnit("L","litre",ChemistryUnitDimension.Volume,.001), ChemistryUnit("mL","millilitre",ChemistryUnitDimension.Volume,1e-6),
        ChemistryUnit("mol/m³","mole per cubic metre",ChemistryUnitDimension.Concentration), ChemistryUnit("mol/L","mole per litre",ChemistryUnitDimension.Concentration,1000.0),
        ChemistryUnit("Pa","pascal",ChemistryUnitDimension.Pressure), ChemistryUnit("kPa","kilopascal",ChemistryUnitDimension.Pressure,1000.0), ChemistryUnit("atm","standard atmosphere",ChemistryUnitDimension.Pressure,101325.0),
        ChemistryUnit("K","kelvin",ChemistryUnitDimension.Temperature), ChemistryUnit("°C","degree Celsius",ChemistryUnitDimension.Temperature,1.0,273.15),
        ChemistryUnit("J","joule",ChemistryUnitDimension.Energy), ChemistryUnit("kJ","kilojoule",ChemistryUnitDimension.Energy,1000.0), ChemistryUnit("J/mol","joule per mole",ChemistryUnitDimension.Enthalpy),
        ChemistryUnit("J/(mol·K)","joule per mole kelvin",ChemistryUnitDimension.Entropy), ChemistryUnit("mol/(L·s)","mole per litre second",ChemistryUnitDimension.Rate),
        ChemistryUnit("S/m","siemens per metre",ChemistryUnitDimension.Conductivity), ChemistryUnit("S·m²/mol","siemens square metre per mole",ChemistryUnitDimension.MolarConductivity),
        ChemistryUnit("AU","absorbance unit",ChemistryUnitDimension.Absorbance), ChemistryUnit("m","metre",ChemistryUnitDimension.Wavelength), ChemistryUnit("nm","nanometre",ChemistryUnitDimension.Wavelength,1e-9),
        ChemistryUnit("Hz","hertz",ChemistryUnitDimension.Frequency), ChemistryUnit("V","volt",ChemistryUnitDimension.ElectrodePotential),
    )
    private val bySymbol = units.associateBy { it.symbol }
    fun convert(value: Double, from: String, to: String): Double { require(value.isFinite()) { "Enter a finite value." }; val source=requireNotNull(bySymbol[from]){"Unknown unit: $from"}; val target=requireNotNull(bySymbol[to]){"Unknown unit: $to"}; require(source.dimension==target.dimension){"Units must describe the same chemical quantity."}; val result=target.fromSi(source.toSi(value)); require(result.isFinite()); return result }
}

