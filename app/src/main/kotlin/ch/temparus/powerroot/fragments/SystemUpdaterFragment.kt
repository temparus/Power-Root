package ch.temparus.powerroot.fragments

import android.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ch.temparus.powerroot.R


/**
 * A simple [Fragment] subclass.
 * Use the [SystemUpdaterFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SystemUpdaterFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO : ...
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater!!.inflate(R.layout.fragment_system_updater, container, false)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment SystemUpdaterFragment.
         */
        fun newInstance(): SystemUpdaterFragment {
            return SystemUpdaterFragment()
        }
    }
}// Required empty public constructor
