import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TicketComp } from './ticket-comp';

describe('TicketComp', () => {
  let component: TicketComp;
  let fixture: ComponentFixture<TicketComp>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TicketComp]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TicketComp);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
