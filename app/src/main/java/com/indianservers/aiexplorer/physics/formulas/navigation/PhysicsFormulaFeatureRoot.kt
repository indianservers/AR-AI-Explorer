package com.indianservers.aiexplorer.physics.formulas.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.indianservers.aiexplorer.physics.formulas.ui.*

@Composable fun PhysicsFormulaFeatureRoot(onExit:()->Unit){val vm=remember{PhysicsFormulaViewModel()};BackHandler{if(!vm.back())onExit()};when(val route=vm.state.route){PhysicsFormulaRoute.Home->PhysicsFormulaHome(vm,onExit);is PhysicsFormulaRoute.Category->PhysicsFormulaCategoryPage(vm,route.categoryId);is PhysicsFormulaRoute.Subcategory->PhysicsFormulaSubcategoryPage(vm,route.subcategoryId);is PhysicsFormulaRoute.Detail->PhysicsFormulaDetailPage(vm,route.formulaId);PhysicsFormulaRoute.Search->PhysicsFormulaSearchPage(vm);PhysicsFormulaRoute.Bookmarks->PhysicsFormulaBookmarksPage(vm);PhysicsFormulaRoute.Revision->PhysicsFormulaPlannedPage(vm,"Revision modes arrive in Phase 6; formula browsing and bookmarks are already active.");is PhysicsFormulaRoute.Quiz->PhysicsFormulaPlannedPage(vm,"Physics formula quizzes arrive in Phase 6.");is PhysicsFormulaRoute.Calculator->PhysicsFormulaPlannedPage(vm,"The validated calculator contract is ready; interactive solving arrives in Phase 3.")}}

