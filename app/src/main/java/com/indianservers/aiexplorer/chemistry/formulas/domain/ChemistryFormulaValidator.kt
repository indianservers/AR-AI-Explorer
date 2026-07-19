package com.indianservers.aiexplorer.chemistry.formulas.domain

import com.indianservers.aiexplorer.chemistry.formulas.model.*

object ChemistryFormulaValidator {
    fun validate(catalogue: ChemistryFormulaCatalogue): ChemistryFormulaValidationReport {
        val errors=mutableListOf<String>(); val warnings=mutableListOf<String>()
        fun duplicate(label:String,ids:List<String>){if(ids.size!=ids.toSet().size) errors+="Duplicate $label ID."}
        duplicate("category",catalogue.categories.map{it.id});duplicate("subcategory",catalogue.subcategories.map{it.id});duplicate("formula",catalogue.formulas.map{it.id})
        val categoryIds=catalogue.categories.map{it.id}.toSet();val subcategories=catalogue.subcategories.associateBy{it.id};val formulaIds=catalogue.formulas.map{it.id}.toSet()
        catalogue.categories.forEach{if(it.subcategoryIds.any{id->id !in subcategories})errors+="${it.id} has a broken subcategory link."}
        catalogue.subcategories.forEach{if(it.categoryId !in categoryIds)errors+="${it.id} has a broken category link."}
        catalogue.formulas.forEach{formula->
            if(formula.categoryId !in categoryIds||subcategories[formula.subcategoryId]?.categoryId!=formula.categoryId)errors+="${formula.id} has a broken hierarchy link."
            if(formula.equation.isBlank()||formula.spokenEquation.isBlank()||formula.variables.isEmpty())errors+="${formula.id} has incomplete display metadata."
            if(formula.variables.map{it.id}.toSet().size!=formula.variables.size||formula.variables.any{it.symbol.isBlank()||it.spokenName.isBlank()})errors+="${formula.id} has invalid variables."
            formula.calculator?.let{if((it.targetVariableIds+it.requiredVariableIds).any{id->id !in formula.variables.map{v->v.id}})errors+="${formula.id} has an invalid calculator definition."}
            if(formula.relatedFormulaIds.any{it !in formulaIds})errors+="${formula.id} has a broken related formula link."
            if((formula.title.contains("pH",true)||formula.title.contains("Nernst",true))&&formula.assumptions.none{it.contains("temperature",true)})errors+="${formula.id} must state temperature assumptions."
        }
        if(catalogue.categories.size!=26)warnings+="Expected 26 requested Chemistry categories."
        if(catalogue.schemaVersion<1)errors+="Invalid schema version."
        return ChemistryFormulaValidationReport(errors,warnings)
    }
}

