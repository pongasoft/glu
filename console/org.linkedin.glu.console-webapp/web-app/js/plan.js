/*
 * Copyright (c) 2014 Yan Pujante
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

/**
 * Execute rest call to create the plan
 */
function createPlan(fabric,
                    url,
                    selectedPlanInputName,
                    maxParallelStepsCountSelector,
                    planPreviewSelector)
{
  selectedPlanInputName = selectedPlanInputName || 'planDetails';
  maxParallelStepsCountSelector = maxParallelStepsCountSelector || '#maxParallelStepsCount';
  planPreviewSelector = planPreviewSelector || '#plan-preview';

  var ajaxData = {
    fabric: fabric
  };

  // first the selected plan
  var planDetails = $('input[name=' + selectedPlanInputName + ']:checked').val();
  if(planDetails)
    ajaxData.json = planDetails;
  else
    return; // nothing selected yet...

  var maxParallelStepsCount = parseInt($(maxParallelStepsCountSelector).val());

  if(isNaN(maxParallelStepsCount))
    maxParallelStepsCount = 0;
  if(maxParallelStepsCount > 0)
    ajaxData.maxParallelStepsCount = maxParallelStepsCount;


  $.ajax({
           type: 'POST',
           data: ajaxData,
           url: url,
           success:function(data,textStatus){ $(planPreviewSelector).html(data);},
           error:function(XMLHttpRequest,textStatus,errorThrown){}
         });
}