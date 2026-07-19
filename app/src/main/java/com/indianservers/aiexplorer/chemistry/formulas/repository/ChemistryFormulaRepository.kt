package com.indianservers.aiexplorer.chemistry.formulas.repository

import com.indianservers.aiexplorer.chemistry.formulas.data.BundledChemistryFormulaData
import com.indianservers.aiexplorer.chemistry.formulas.domain.ChemistryFormulaValidator
import com.indianservers.aiexplorer.chemistry.formulas.model.*

interface ChemistryFormulaRepository {
    fun getCategories():List<ChemistryFormulaCategory>;fun getCategory(id:String):ChemistryFormulaCategory?;fun getSubcategories(categoryId:String):List<ChemistryFormulaSubcategory>;fun getSubcategory(id:String):ChemistryFormulaSubcategory?;fun getFormulas(subcategoryId:String):List<ChemistryFormula>;fun getFormula(id:String):ChemistryFormula?
    fun search(query:String,filters:ChemistryFormulaFilters=ChemistryFormulaFilters(),bookmarked:Set<String> = emptySet(),recent:List<String> = emptyList()):List<ChemistryFormula>;fun getRelatedFormulas(id:String):List<ChemistryFormula>;fun validate():ChemistryFormulaValidationReport
}
class OfflineChemistryFormulaRepository(private val catalogue:ChemistryFormulaCatalogue=BundledChemistryFormulaData.catalogue):ChemistryFormulaRepository{
    private val categories=catalogue.categories.associateBy{it.id};private val subcategories=catalogue.subcategories.associateBy{it.id};private val formulas=catalogue.formulas.associateBy{it.id}
    init{require(ChemistryFormulaValidator.validate(catalogue).valid)}
    override fun getCategories()=catalogue.categories;override fun getCategory(id:String)=categories[id];override fun getSubcategories(categoryId:String)=catalogue.subcategories.filter{it.categoryId==categoryId};override fun getSubcategory(id:String)=subcategories[id];override fun getFormulas(subcategoryId:String)=catalogue.formulas.filter{it.subcategoryId==subcategoryId};override fun getFormula(id:String)=formulas[id]
    override fun search(query:String,filters:ChemistryFormulaFilters,bookmarked:Set<String>,recent:List<String>):List<ChemistryFormula>{val needle=query.trim().lowercase().replace("×","*").replace("²","^2").replace(" ","");return catalogue.formulas.filter{formula->val category=categories[formula.categoryId]?.title.orEmpty();val subcategory=subcategories[formula.subcategoryId]?.title.orEmpty();val haystack=listOf(formula.title,formula.searchableEquation,formula.spokenEquation,formula.description,category,subcategory,formula.variables.joinToString{"${it.symbol} ${it.spokenName}"},formula.keywords.joinToString()).joinToString().lowercase().replace(" ","");(needle.isEmpty()||needle in haystack)&&formula.minimumLevel.rank<=filters.level.rank&&(filters.categoryId==null||formula.categoryId==filters.categoryId)&&(filters.subcategoryId==null||formula.subcategoryId==filters.subcategoryId)&&(!filters.calculatorOnly||formula.calculator!=null)&&(!filters.derivationOnly||formula.derivationSteps.isNotEmpty())&&(!filters.bookmarkedOnly||formula.id in bookmarked)&&(!filters.recentOnly||formula.id in recent)}}
    override fun getRelatedFormulas(id:String)=formulas[id]?.relatedFormulaIds.orEmpty().mapNotNull(formulas::get);override fun validate()=ChemistryFormulaValidator.validate(catalogue)
}

