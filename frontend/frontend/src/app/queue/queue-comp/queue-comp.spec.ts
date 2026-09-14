import { ComponentFixture, TestBed } from '@angular/core/testing';

import { QueueComp } from './queue-comp';

describe('QueueComp', () => {
  let component: QueueComp;
  let fixture: ComponentFixture<QueueComp>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [QueueComp]
    })
    .compileComponents();

    fixture = TestBed.createComponent(QueueComp);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
