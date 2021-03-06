/*-
 * #%L
 * Arcade Analytics
 * %%
 * Copyright (C) 2018 - 2019 ArcadeAnalytics
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
import { browser, element, by, $ } from 'protractor';
import { NavBarPage } from './../page-objects/jhi-page-objects';
const path = require('path');

describe('DataSet e2e test', () => {

    let navBarPage: NavBarPage;
    let dataSetDialogPage: DataSetDialogPage;
    let dataSetComponentsPage: DataSetComponentsPage;
    const fileToUpload = '../../../../main/webapp/content/images/logo-jhipster.png';
    const absolutePath = path.resolve(__dirname, fileToUpload);
    

    beforeAll(() => {
        browser.get('/');
        browser.waitForAngular();
        navBarPage = new NavBarPage();
        navBarPage.getSignInPage().autoSignInUsing('admin', 'admin');
        browser.waitForAngular();
    });

    it('should load DataSets', () => {
        navBarPage.goToEntity('data-set');
        dataSetComponentsPage = new DataSetComponentsPage();
        expect(dataSetComponentsPage.getTitle()).toMatch(/arcadeanalyticsApp.dataSet.home.title/);

    });

    it('should load create DataSet dialog', () => {
        dataSetComponentsPage.clickOnCreateButton();
        dataSetDialogPage = new DataSetDialogPage();
        expect(dataSetDialogPage.getModalTitle()).toMatch(/arcadeanalyticsApp.dataSet.home.createOrEditLabel/);
        dataSetDialogPage.close();
    });

    it('should create and save DataSets', () => {
        dataSetComponentsPage.clickOnCreateButton();
        dataSetDialogPage.setNameInput('name');
        expect(dataSetDialogPage.getNameInput()).toMatch('name');
        dataSetDialogPage.save();
        expect(dataSetDialogPage.getSaveButton().isPresent()).toBeFalsy();
    }); 

    afterAll(() => {
        navBarPage.autoSignOut();
    });
});

export class DataSetComponentsPage {
    createButton = element(by.css('.jh-create-entity'));
    title = element.all(by.css('jhi-data-set div h2 span')).first();

    clickOnCreateButton() {
        return this.createButton.click();
    }

    getTitle() {
        return this.title.getAttribute('jhiTranslate');
    }
}

export class DataSetDialogPage {
    modalTitle = element(by.css('h4#myDataSetLabel'));
    saveButton = element(by.css('.modal-footer .btn.btn-primary'));
    closeButton = element(by.css('button.close'));
    nameInput = element(by.css('input#field_name'));

    getModalTitle() {
        return this.modalTitle.getAttribute('jhiTranslate');
    }

    setNameInput = function (name) {
        this.nameInput.sendKeys(name);
    }

    getNameInput = function () {
        return this.nameInput.getAttribute('value');
    }

    save() {
        this.saveButton.click();
    }

    close() {
        this.closeButton.click();
    }

    getSaveButton() {
        return this.saveButton;
    }
}
