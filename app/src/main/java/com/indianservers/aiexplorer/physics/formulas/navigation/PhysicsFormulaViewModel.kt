package com.indianservers.aiexplorer.physics.formulas.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.indianservers.aiexplorer.physics.formulas.model.*
import com.indianservers.aiexplorer.physics.formulas.repository.OfflinePhysicsFormulaRepository
import com.indianservers.aiexplorer.physics.formulas.repository.PhysicsFormulaRepository

data class PhysicsFormulaUiState(val stack:List<PhysicsFormulaRoute> = listOf(PhysicsFormulaRoute.Home),val query:String="",val filters:PhysicsFormulaFilters=PhysicsFormulaFilters(),val results:List<PhysicsFormula> = emptyList(),val bookmarks:Set<String> = emptySet(),val recentlyViewed:List<String> = emptyList()) { val route get()=stack.last() }
class PhysicsFormulaViewModel(val repository:PhysicsFormulaRepository=OfflinePhysicsFormulaRepository()){
    var state by mutableStateOf(PhysicsFormulaUiState(results=repository.search("")));private set
    fun navigate(route:PhysicsFormulaRoute){val recent=if(route is PhysicsFormulaRoute.Detail)(listOf(route.formulaId)+state.recentlyViewed.filterNot{it==route.formulaId}).take(20)else state.recentlyViewed;state=state.copy(stack=state.stack+route,recentlyViewed=recent)}
    fun back():Boolean{if(state.stack.size<=1)return false;state=state.copy(stack=state.stack.dropLast(1));return true}
    fun home(){state=state.copy(stack=listOf(PhysicsFormulaRoute.Home))}
    fun search(query:String){state=state.copy(query=query,results=repository.search(query,state.filters,state.bookmarks,state.recentlyViewed))}
    fun setLevel(level:PhysicsFormulaLevel){updateFilters(state.filters.copy(level=level))}
    fun toggleCalculator(){updateFilters(state.filters.copy(calculatorOnly=!state.filters.calculatorOnly))}
    fun toggleDerivation(){updateFilters(state.filters.copy(derivationOnly=!state.filters.derivationOnly))}
    fun toggleBookmarksFilter(){updateFilters(state.filters.copy(bookmarkedOnly=!state.filters.bookmarkedOnly))}
    fun toggleBookmark(id:String){val marks=if(id in state.bookmarks)state.bookmarks-id else state.bookmarks+id;state=state.copy(bookmarks=marks,results=repository.search(state.query,state.filters,marks,state.recentlyViewed))}
    private fun updateFilters(filters:PhysicsFormulaFilters){state=state.copy(filters=filters,results=repository.search(state.query,filters,state.bookmarks,state.recentlyViewed))}
}

