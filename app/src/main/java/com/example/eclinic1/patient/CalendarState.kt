import com.example.eclinic1.patient.Appointment
import java.time.LocalDate

data class CalendarState(
    val dates: List<LocalDate> = emptyList(),
    val selectedDate: LocalDate = LocalDate.now(),
    val appointments: List<Appointment> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)