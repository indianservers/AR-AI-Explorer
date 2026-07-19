package com.indianservers.aiexplorer.chemistry.formulas.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.indianservers.aiexplorer.chemistry.formulas.ui.*

@Composable fun ChemistryFormulaFeatureRoot(onExit:()->Unit){val vm=remember{ChemistryFormulaViewModel()};BackHandler{if(!vm.back())onExit()};when(val route=vm.state.route){ChemistryFormulaRoute.Home->ChemistryFormulaHome(vm,onExit);is ChemistryFormulaRoute.Category->ChemistryFormulaCategoryPage(vm,route.categoryId);is ChemistryFormulaRoute.Subcategory->ChemistryFormulaSubcategoryPage(vm,route.subcategoryId);is ChemistryFormulaRoute.Detail->ChemistryFormulaDetailPage(vm,route.formulaId);ChemistryFormulaRoute.Search->ChemistryFormulaSearchPage(vm);ChemistryFormulaRoute.Bookmarks->ChemistryFormulaBookmarksPage(vm);ChemistryFormulaRoute.Revision->ChemistryFormulaPlannedPage(vm,"Revision modes arrive in Phase 6; formula browsing and bookmarks are active.");is ChemistryFormulaRoute.Quiz->ChemistryFormulaPlannedPage(vm,"Chemistry formula quizzes arrive in Phase 6.");is ChemistryFormulaRoute.Calculator->ChemistryFormulaPlannedPage(vm,"The validated Chemistry calculator contract is ready; interactive solving arrives in Phase 3.")}}

