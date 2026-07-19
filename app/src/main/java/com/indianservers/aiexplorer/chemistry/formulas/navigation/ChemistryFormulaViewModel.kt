package com.indianservers.aiexplorer.chemistry.formulas.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.indianservers.aiexplorer.chemistry.formulas.model.*
import com.indianservers.aiexplorer.chemistry.formulas.repository.ChemistryFormulaRepository
import com.indianservers.aiexplorer.chemistry.formulas.repository.OfflineChemistryFormulaRepository

data class ChemistryFormulaUiState(val stack:List<ChemistryFormulaRoute> = listOf(ChemistryFormulaRoute.Home),val query:String="",val filters:ChemistryFormulaFilters=ChemistryFormulaFilters(),val results:List<ChemistryFormula> = emptyList(),val bookmarks:Set<String> = emptySet(),val recentlyViewed:List<String> = emptyList()){val route get()=stack.last()}
class ChemistryFormulaViewModel(val repository:ChemistryFormulaRepository=OfflineChemistryFormulaRepository()){
    var state by mutableStateOf(ChemistryFormulaUiState(results=repository.search("")));private set
    fun navigate(route:ChemistryFormulaRoute){val recent=if(route is ChemistryFormulaRoute.Detail)(listOf(route.formulaId)+state.recentlyViewed.filterNot{it==route.formulaId}).take(20)else state.recentlyViewed;state=state.copy(stack=state.stack+route,recentlyViewed=recent)}
    fun back():Boolean{if(state.stack.size<=1)return false;state=state.copy(stack=state.stack.dropLast(1));return true};fun home(){state=state.copy(stack=listOf(ChemistryFormulaRoute.Home))}
    fun search(query:String){state=state.copy(query=query,results=repository.search(query,state.filters,state.bookmarks,state.recentlyViewed))};fun setLevel(level:ChemistryFormulaLevel){update(state.filters.copy(level=level))};fun toggleCalculator(){update(state.filters.copy(calculatorOnly=!state.filters.calculatorOnly))};fun toggleDerivation(){update(state.filters.copy(derivationOnly=!state.filters.derivationOnly))};fun toggleBookmarksFilter(){update(state.filters.copy(bookmarkedOnly=!state.filters.bookmarkedOnly))}
    fun toggleBookmark(id:String){val marks=if(id in state.bookmarks)state.bookmarks-id else state.bookmarks+id;state=state.copy(bookmarks=marks,results=repository.search(state.query,state.filters,marks,state.recentlyViewed))};private fun update(filters:ChemistryFormulaFilters){state=state.copy(filters=filters,results=repository.search(state.query,filters,state.bookmarks,state.recentlyViewed))}
}

