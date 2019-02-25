import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { RouterModule } from '@angular/router';

import { ArcadeanalyticsSharedModule } from '../../shared';
import {
    WidgetService,
    WidgetPopupService,
    WidgetComponent,
    WidgetDetailComponent,
    WidgetEmbedComponent,
    WidgetNewDialogComponent,
    WidgetNewPopupComponent,
    WidgetEditPopupComponent,
    WidgetEditDialogComponent,
    WidgetDeletePopupComponent,
    WidgetDeleteDialogComponent,
    widgetRoute,
    widgetPopupRoute,
    WidgetResolvePagingParams,
    GraphWidgetComponent,
    TableWidgetComponent,
    IndependentPieChartWidgetComponent,
    SecondaryPieChartWidgetComponent,
    IndependentBarChartWidgetComponent,
    SecondaryBarChartWidgetComponent,
    DataSourceInfoComponent,
    TimelineComponent,
    SaveOnExitPopupComponent,
    PerformQueryModalComponent,
    PerformTraverseModalComponent,
    ShortestPathConfigModalComponent,
    PageRankConfigModalComponent,
    CentralityConfigModalComponent,
    EmbedResourceModalComponent,
    AddEdgeModalComponent,
    AddNodeModalComponent,
    TextEditorWidgetComponent
} from './';
import {
    VertexMenuComponent,
    EdgeMenuComponent,
    TraverseMenuComponent,
    PropertiesComponent,
    LabelComponent,
    ShapeComponent,
    FulltextSearchComponent,
    FilterMenuComponent,
    SnapshotMenuComponent
} from './implementation/';
import { TableComponent } from './implementation/util-component/table';

const ENTITY_STATES = [
    ...widgetRoute,
    ...widgetPopupRoute,
];

@NgModule({
    imports: [
        ArcadeanalyticsSharedModule,
        RouterModule.forChild(ENTITY_STATES)
    ],
    declarations: [
        WidgetComponent,
        WidgetDetailComponent,
        WidgetEmbedComponent,
        WidgetNewDialogComponent,
        WidgetNewPopupComponent,
        WidgetEditPopupComponent,
        WidgetEditDialogComponent,
        WidgetDeletePopupComponent,
        WidgetDeleteDialogComponent,
        TextEditorWidgetComponent,
        GraphWidgetComponent,
        TableWidgetComponent,
        IndependentPieChartWidgetComponent,
        SecondaryPieChartWidgetComponent,
        IndependentBarChartWidgetComponent,
        SecondaryBarChartWidgetComponent,
        TableComponent,
        DataSourceInfoComponent,
        VertexMenuComponent,
        EdgeMenuComponent,
        PropertiesComponent,
        LabelComponent,
        ShapeComponent,
        TraverseMenuComponent,
        FulltextSearchComponent,
        FilterMenuComponent,
        SnapshotMenuComponent,
        TimelineComponent,
        SaveOnExitPopupComponent,
        PerformQueryModalComponent,
        PerformTraverseModalComponent,
        ShortestPathConfigModalComponent,
        PageRankConfigModalComponent,
        CentralityConfigModalComponent,
        EmbedResourceModalComponent,
        AddEdgeModalComponent,
        AddNodeModalComponent
    ],
    entryComponents: [
        WidgetComponent,
        WidgetNewDialogComponent,
        WidgetNewPopupComponent,
        WidgetEditPopupComponent,
        WidgetEditDialogComponent,
        WidgetDeleteDialogComponent,
        WidgetDeletePopupComponent,
        TextEditorWidgetComponent,
        GraphWidgetComponent,
        TableWidgetComponent,
        IndependentPieChartWidgetComponent,
        SecondaryPieChartWidgetComponent,
        IndependentBarChartWidgetComponent,
        SecondaryBarChartWidgetComponent,
        TableComponent,
        DataSourceInfoComponent,
        VertexMenuComponent,
        EdgeMenuComponent,
        PropertiesComponent,
        LabelComponent,
        ShapeComponent,
        TraverseMenuComponent,
        FulltextSearchComponent,
        FilterMenuComponent,
        SnapshotMenuComponent,
        TimelineComponent,
        SaveOnExitPopupComponent,
        PerformQueryModalComponent,
        PerformTraverseModalComponent,
        ShortestPathConfigModalComponent,
        PageRankConfigModalComponent,
        CentralityConfigModalComponent,
        EmbedResourceModalComponent,
        AddEdgeModalComponent,
        AddNodeModalComponent
    ],
    providers: [
        WidgetService,
        WidgetPopupService,
        WidgetResolvePagingParams,
    ],
    exports: [
        TextEditorWidgetComponent,
        GraphWidgetComponent,
        TableWidgetComponent,
        IndependentPieChartWidgetComponent,
        SecondaryPieChartWidgetComponent,
        IndependentBarChartWidgetComponent,
        SecondaryBarChartWidgetComponent,
        TableComponent,
        DataSourceInfoComponent,
        TimelineComponent
    ],
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class ArcadeanalyticsWidgetModule { }
