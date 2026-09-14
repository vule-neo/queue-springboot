// Oblik onoga sto backend salje u QueueResponse.
// interface a ne class: JSON iz http.get<Queue[]> pravi obicne objekte,
// nikad prave instance klase - interface to ne lazira.
export interface Queue {
  id: number;
  locationId: number;
  serviceId: number;
  prefix: string;
  open: boolean;
  lastNumber: number;
  lastNumberDate: string | null;   // jedino polje koje backend stvarno salje kao null
  createdAt: string;               // JSON nema datume, samo tekst
}
